package com.jimmy.prototype.http.async.consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentInputStream;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Created by zengdejun on 2017/11/8.
 */
public class ResponseConsumer extends AbstractAsyncResponseConsumer<String> {
    private static Logger logger = LoggerFactory.getLogger(ResponseConsumer.class);
    private String result;
    private String msgPrefix;
    private Exception ex;
    private int status;
    private String statusMsg;

    private SimpleInputBuffer buf;

    public ResponseConsumer(String dsp, String host) {
        super();
        msgPrefix = String.format("%s %s ", dsp, host);
    }

    @Override
    protected void onResponseReceived(HttpResponse response) throws HttpException, IOException {
        if (response != null && response.getStatusLine() != null) {
            this.status = response.getStatusLine().getStatusCode();
            this.statusMsg = "http.status." + this.status;
        }
    }

    @Override
    protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        Asserts.notNull(this.buf, "Content buffer");
        this.buf.consumeContent(decoder);
    }

    @Override
    protected void onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException {
        if (entity == null) {
            ex = new Exception(msgPrefix + " no response entity");
        }
        long len = entity.getContentLength();
        if (len > Integer.MAX_VALUE) {
            throw new ContentTooLongException("Entity content is too long: " + len);
        }
        if (len < 0) {
            len = 10240; // default 10KB
        }
        this.buf = new SimpleInputBuffer((int) len, new HeapByteBufferAllocator());
    }

    @Override
    protected String buildResult(HttpContext context) throws Exception {
        if (status == HttpStatus.SC_OK) {
            StringWriter writer = new StringWriter();
            try{
                InputStream inputStream = new ContentInputStream(this.buf);
                IOUtils.copy(inputStream, writer, "UTF-8");
                result = writer.toString();
            }finally {
                writer.close();
            }
        }
        if (ex != null) {
            throw ex;
        } else if (status == 0) {
            throw new Exception(msgPrefix + " HttpResponse is null");
        } else if (status == HttpStatus.SC_OK && StringUtils.isBlank(result)) {
            throw new Exception(msgPrefix + " no response entity");
        } else if (status != HttpStatus.SC_OK && status != HttpStatus.SC_NO_CONTENT) {
            throw new NoHttpResponseException(msgPrefix + statusMsg);
        }
//        logger.debug("Result from {}, response status:{}, bidResponse:{}", msgPrefix, status, result);
        return result;
    }

    @Override
    protected void releaseResources() {
        this.buf = null;
    }
}
