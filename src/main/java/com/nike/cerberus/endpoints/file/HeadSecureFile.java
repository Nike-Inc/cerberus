/*
 * Copyright (c) 2018 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nike.cerberus.endpoints.file;

import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.SecureDataRequestService;
import com.nike.cerberus.domain.SecureDataRequestInfo;
import com.nike.cerberus.domain.SecureFileSummary;
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.endpoints.RiposteEndpoint;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import com.nike.riposte.util.MultiMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.endpoints.file.WriteSecureFile.BASE_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

@RiposteEndpoint
public class HeadSecureFile extends AuditableEventEndpoint<Void, Void> {

    private final SecureDataService secureDataService;
    private final SecureDataRequestService secureDataRequestService;

    @Inject
    protected HeadSecureFile(SecureDataService secureDataService,
                             SecureDataRequestService secureDataRequestService) {

        this.secureDataService = secureDataService;
        this.secureDataRequestService = secureDataRequestService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(RequestInfo<Void> request,
                                                           Executor longRunningTaskExecutor,
                                                           ChannelHandlerContext ctx) {

        SecureDataRequestInfo requestInfo = secureDataRequestService.parseAndValidateRequest(request);
        ResponseInfo<Void> response;

        Optional<SecureFileSummary> secureFileOpt = secureDataService.readFileMetadataOnly(requestInfo.getSdbId(), requestInfo.getPath());

        if (! secureFileOpt.isPresent()) {
            throw new ApiException.Builder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .build();
        } else {
            SecureFileSummary secureFileSummary = secureFileOpt.get();
            String mimeType = getMimeTypeFromExtension(secureFileSummary.getName());

            response = ResponseInfo.<Void>newBuilder()
                    .withHeaders(new DefaultHttpHeaders()
                            .add("Content-Length", secureFileSummary.getSizeInBytes())
                            .add("Content-Disposition", String.format("attachment; filename=\"%s\"", secureFileSummary.getName())))
                    .withDesiredContentWriterMimeType(mimeType)
                    .withHttpStatusCode(HttpResponseStatus.OK.code())
                    .build();
        }

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> response, ctx),
                longRunningTaskExecutor);
    }

    private String getMimeTypeFromExtension(String filename) {
        Buffer mimeType = new MimeTypes().getMimeByExtension(filename);

        return mimeType == null ?
                APPLICATION_OCTET_STREAM :
                mimeType.toString(StandardCharsets.UTF_8);
    }

    @Override
    public Matcher requestMatcher() {
        return MultiMatcher.match(
                Sets.newHashSet(
                        String.format("%s/**", BASE_PATH),
                        BASE_PATH
                ),
                HttpMethod.HEAD
        );
    }

    @Override
    protected String describeActionForAuditEvent(RequestInfo<Void> request) {
        String path = request.getPath();

        return String.format("Reading file metadata for path: %s", path);
    }
}
