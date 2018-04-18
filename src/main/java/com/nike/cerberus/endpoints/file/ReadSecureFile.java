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
import com.nike.cerberus.domain.SecureFile;
import com.nike.cerberus.domain.SecureFileVersion;
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import com.nike.riposte.util.MultiMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.endpoints.file.WriteSecureFile.BASE_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

public class ReadSecureFile extends AuditableEventEndpoint<Void, byte[]> {

    private final SecureDataService secureDataService;
    private final SecureDataVersionService secureDataVersionService;
    private final SecureDataRequestService secureDataRequestService;

    @Inject
    protected ReadSecureFile(SecureDataService secureDataService,
                             SecureDataRequestService secureDataRequestService,
                             SecureDataVersionService secureDataVersionService) {

        this.secureDataService = secureDataService;
        this.secureDataRequestService = secureDataRequestService;
        this.secureDataVersionService = secureDataVersionService;
    }

    @Override
    public CompletableFuture<ResponseInfo<byte[]>> doExecute(RequestInfo<Void> request,
                                                                  Executor longRunningTaskExecutor,
                                                                  ChannelHandlerContext ctx) {

        SecureDataRequestInfo requestInfo = secureDataRequestService.parseAndValidateRequest(request);
        ResponseInfo<byte[]> response;

        if (StringUtils.isNotBlank(request.getQueryParamSingle("versionId"))) {
            String versionId = request.getQueryParamSingle("versionId");
            response = readSecureDataVersion(requestInfo, versionId);
        } else {
            Optional<SecureFile> secureFileOpt = secureDataService.readFile(requestInfo.getPath());

            if (! secureFileOpt.isPresent()) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                        .build();
            } else {
                SecureFile secureFile = secureFileOpt.get();
                byte[] fileContents = secureFile.getData();
                String mimeType = getMimeTypeFromExtension(secureFile.getName());

                response = ResponseInfo.<byte[]>newBuilder()
                        .withHeaders(new DefaultHttpHeaders()
                                .add("Content-Length", secureFile.getSizeInBytes())
                                .add("Content-Disposition", String.format("attachment; filename=\"%s\"", secureFile.getName())))
                        .withDesiredContentWriterMimeType(mimeType)
                        .withContentForFullResponse(fileContents)
                        .withHttpStatusCode(HttpResponseStatus.OK.code())
                        .build();
            }
        }

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> response, ctx),
                longRunningTaskExecutor);
    }

    private ResponseInfo<byte[]> readSecureDataVersion(SecureDataRequestInfo requestInfo,
                                                       String versionId) {
        Optional<SecureFileVersion> secureFileVersionOpt = secureDataVersionService.getSecureFileVersionById(
                versionId,
                requestInfo.getCategory(),
                requestInfo.getPath());

        if (! secureFileVersionOpt.isPresent()) {
            throw ApiException.newBuilder()
                    .withExceptionMessage(String.format("Version does not exist for path: %s, ID: %s", versionId, requestInfo.getPath()))
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .build();
        }

        SecureFileVersion secureFileVersion = secureFileVersionOpt.get();
        byte[] fileContents = secureFileVersion.getData();
        String mimeType = getMimeTypeFromExtension(secureFileVersion.getName());

        return ResponseInfo.<byte[]>newBuilder()
                .withHeaders(new DefaultHttpHeaders()
                        .add("Content-Length", secureFileVersion.getSizeInBytes())
                        .add("Content-Disposition", String.format("attachment; filename=\"%s\"", secureFileVersion.getName())))
                .withDesiredContentWriterMimeType(mimeType)
                .withContentForFullResponse(fileContents)
                .withHttpStatusCode(HttpResponseStatus.OK.code())
                .build();
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
                HttpMethod.GET
        );
    }

    @Override
    protected String describeActionForAuditEvent(RequestInfo<Void> request) {
        String path = request.getPath();
         if (StringUtils.isNotBlank(request.getQueryParamSingle("versionId"))) {
            String versionId = request.getQueryParamSingle("versionId");
            return String.format("Reading secure file with path: %s and version id: %s", path, versionId);
        } else {
            return String.format("Reading secure file with path: %s", path);
        }
    }
}
