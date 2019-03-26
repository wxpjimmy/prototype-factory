package com.jimmy.prototype.http.async.consumer;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by zengdejun on 2017/11/15.
 */
public class BytesResponseConsumer extends AbstractAsyncResponseConsumer<byte[]> {
    private static Logger logger = LoggerFactory.getLogger(BytesResponseConsumer.class);
    private byte[] result;
    private String msgPrefix;
    private volatile HttpResponse response;
    private volatile SimpleInputBuffer buf;

    public BytesResponseConsumer(String dsp, String host) {
        super();
        msgPrefix = String.format("%s %s ", dsp, host);
    }

    @Override
    protected void onResponseReceived(final HttpResponse response) throws IOException {
        this.response = response;
    }

    @Override
    protected void onEntityEnclosed(
            final HttpEntity entity, final ContentType contentType) throws IOException {
        long len = entity.getContentLength();
        if (len > Integer.MAX_VALUE) {
            throw new ContentTooLongException("Entity content is too long: " + len);
        }
        if (len < 0) {
            len = 10240;
        }
        this.buf = new SimpleInputBuffer((int) len, new HeapByteBufferAllocator());
        this.response.setEntity(new ContentBufferEntity(entity, this.buf));
    }

    @Override
    protected void onContentReceived(
            final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
        Asserts.notNull(this.buf, "Content buffer");
        this.buf.consumeContent(decoder);
    }

    @Override
    protected void releaseResources() {
        this.response = null;
        this.buf = null;
    }

    @Override
    protected byte[] buildResult(HttpContext context) throws Exception {
        int status = 0;
        String statusMsg = "";
        if (response != null && response.getStatusLine() != null) {
            status = response.getStatusLine().getStatusCode();
            statusMsg = "http.status." + status;
        }

        if (status == HttpStatus.SC_OK) {
            ByteArrayOutputStream babuf = new ByteArrayOutputStream();
            try{
                response.getEntity().writeTo(babuf);
                result = babuf.toByteArray();
            } catch (Exception e) {
                logger.error("Fail to get Entity content", e);
            }
            finally {
                IOUtils.closeQuietly(babuf);
            }
        }
        if (status == 0) {
            throw new Exception(msgPrefix + " HttpResponse is null");
        } else if (status == HttpStatus.SC_OK && result == null) {
            throw new Exception(msgPrefix + " no response entity");
        } else if (status != HttpStatus.SC_OK && status != HttpStatus.SC_NO_CONTENT) {
            throw new NoHttpResponseException(msgPrefix + statusMsg);
        }
        logger.debug("Result from {}, response status:{}, bidResponse:{}", msgPrefix, status, result);
        return result;
    }

}
