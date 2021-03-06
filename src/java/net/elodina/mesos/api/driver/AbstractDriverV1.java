package net.elodina.mesos.api.driver;

import com.google.protobuf.GeneratedMessage;
import com.googlecode.protobuf.format.JsonFormat;
import net.elodina.mesos.util.Period;
import net.elodina.mesos.util.Request;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public abstract class AbstractDriverV1 {
    protected Logger logger = Logger.getLogger(getClass());

    protected String url;
    protected Period reconnectDelay = new Period("5s");

    protected volatile State state = State.CREATED;
    protected volatile String streamId;

    protected AbstractDriverV1(String url) {
        this.url = url;
    }

    public Period getReconnectDelay() { return reconnectDelay; }
    public void setReconnectDelay(Period reconnectDelay) { this.reconnectDelay = reconnectDelay; }

    public State getState() { return state; }

    public boolean run()  {
        if (state != State.CREATED) throw new IllegalStateException("!created");

        while (state != State.STOPPED) {
            try {
                state = State.STARTED;
                run0();
            } catch (IOException | DriverException e) {
                if (e instanceof DriverException && ((DriverException)e).isUnrecoverable()) {
                    logger.debug(e + ", stopping");
                    return false;
                }

                logger.debug(e + ", reconnecting after " + reconnectDelay);

                try { Thread.sleep(reconnectDelay.ms()); }
                catch (InterruptedException ie) { break; }
            }
        }

        return true;
    }

    private void run0() throws IOException {
        try (Request request = new Request(url)) {
            request.method(Request.Method.POST)
                .contentType("application/json")
                .accept("application/json");

            StringWriter requestJson = new StringWriter();
            new JsonFormat().print(subscribeCall(), requestJson);
            request.body(requestJson.toString().getBytes("utf-8"));
            logger.debug("[subscribe] " + requestJson);

            Request.Response response = request.send(true);
            if (response.code() != 200)
                throw new DriverException("Response: " + response.code() + " - " + response.message() + (response.body() != null ? ": " + new String(response.body()) : ""));

            streamId = response.header("Mesos-Stream-Id");

            InputStream stream = response.stream();
            while (state != State.STOPPED) {
                int size = readChunkSize(stream);
                byte[] buffer = readChunk(stream, size);

                String json = new String(buffer);
                logger.debug("[event] " + json);

                json = json
                    .replaceAll("\\\\/", "/")            // wrong slash escaping bug
                    .replaceAll("\\{\\}", "{\"_t\":1}"); // expected identified } bug

                onEvent(json);
            }

            stream.close();
        } finally {
            streamId = null;
        }
    }

    protected int readChunkSize(InputStream stream) throws IOException {
        byte b;

        String s = "";
        while ((b = (byte) stream.read()) != '\n')
            s += (char)b;

        return Integer.parseInt(s);
    }

    protected byte[] readChunk(InputStream stream, int size) throws IOException {
        byte[] buffer = new byte[size];

        for (int i = 0; i < size; i++)
            buffer[i] = (byte) stream.read();

        return buffer;
    }

    protected abstract GeneratedMessage subscribeCall();

    protected abstract void onEvent(String json);

    protected void sendCall(GeneratedMessage call) {
        try {
            StringWriter body = new StringWriter();
            new JsonFormat().print(call, body);
            logger.debug("[call] " + body);

            Request request = new Request(url)
                .method(Request.Method.POST)
                .contentType("application/json")
                .accept("application/json")
                .body(("" + body).getBytes("utf-8"));

            if (streamId != null) // Mesos 0.25 has no streamId
                request.header("Mesos-Stream-Id", streamId);

            Request.Response response = request.send();
            logger.debug("[response] " + response.code() + " - " + response.message() + (response.body() != null ? ": " + new String(response.body()) : ""));
            if (response.code() != 202)
                throw new DriverException("Response: " + response.code() + " - " + response.message() + (response.body() != null ? ": " + new String(response.body()) : ""));

        } catch (IOException e) {
            throw new DriverException(e);
        }
    }


    protected static String fixUrl(String url) {
        if (!url.startsWith("http://")) url = "http://" + url;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    public enum State {
        CREATED, STARTED, SUBSCRIBED, STOPPED
    }
}
