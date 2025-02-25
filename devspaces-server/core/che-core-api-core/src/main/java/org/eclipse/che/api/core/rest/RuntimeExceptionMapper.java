/*
 * Copyright (c) 2012-2023 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.core.rest;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.inject.Singleton;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception mapper that provide error message with error time.
 *
 * @author Roman Nikitenko
 * @author Sergii Kabashniuk
 */
@Provider
@Singleton
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
  private static final Logger LOG = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

  @Override
  public Response toResponse(RuntimeException exception) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    final String utcTime = dateFormat.format(new Date());
    String message = exception.getMessage();
    final String errorMessage =
        isNullOrEmpty(message)
            ? format("Internal Server Error occurred, error time: %s", utcTime)
            : message;

    LOG.error(errorMessage, exception);

    List<String> trace =
        stream(exception.getStackTrace()).map(StackTraceElement::toString).collect(toList());
    ServiceError serviceError =
        DtoFactory.newDto(ServiceError.class).withMessage(errorMessage).withTrace(trace);
    return Response.serverError()
        .entity(DtoFactory.getInstance().toJson(serviceError))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
