package com.jp.calefaction.service.responses.embed;

public interface EmbedResponseService<I, O> {

    O createEmbedResponse(I input);
}
