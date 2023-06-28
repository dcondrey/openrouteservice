/*
 * This file is part of Openrouteservice.
 *
 * Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, see <https://www.gnu.org/licenses/>.
 */

package org.heigit.ors.api.controllers;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.heigit.ors.api.errors.CommonResponseEntityExceptionHandler;
import org.heigit.ors.routing.APIEnums;
import org.heigit.ors.api.requests.matrix.MatrixRequest;
import org.heigit.ors.api.responses.matrix.json.JSONMatrixResponse;
import org.heigit.ors.exceptions.*;
import org.heigit.ors.matrix.MatrixErrorCodes;
import org.heigit.ors.matrix.MatrixResult;
import org.springdoc.core.annotations.RouterOperation;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@Tag(name = "Matrix Service", description = "Obtain one-to-many, many-to-one and many-to-many matrices for time and distance")
@RequestMapping("/v2/matrix")
@ApiResponse(responseCode = "400", description = "The request is incorrect and therefore can not be processed.")
@ApiResponse(responseCode = "404", description = "An element could not be found. If possible, a more detailed error code is provided.")
@ApiResponse(responseCode = "405", description = "The specified HTTP method is not supported. For more details, refer to the EndPoint documentation.")
@ApiResponse(responseCode = "413", description = "The request is larger than the server is able to process, the data provided in the request exceeds the capacity limit.")
@ApiResponse(responseCode = "500", description = "An unexpected error was encountered and a more detailed error code is provided.")
@ApiResponse(responseCode = "501", description = "Indicates that the server does not support the functionality needed to fulfill the request.")
@ApiResponse(responseCode = "503", description = "The server is currently unavailable due to overload or maintenance.")
public class MatrixAPI {
    static final CommonResponseEntityExceptionHandler errorHandler = new CommonResponseEntityExceptionHandler(MatrixErrorCodes.BASE);

    // generic catch methods - when extra info is provided in the url, the other methods are accessed.
    @GetMapping
    @Operation(summary = "", hidden = true)
    public void getGetMapping() throws MissingParameterException {
        throw new MissingParameterException(MatrixErrorCodes.MISSING_PARAMETER, "profile");
    }

    @PostMapping
    @Operation(summary = "", hidden = true)
    public String getPostMapping() throws MissingParameterException {
        throw new MissingParameterException(MatrixErrorCodes.MISSING_PARAMETER, "profile");
    }

    // Matches any response type that has not been defined
    @PostMapping(value = "/{profile}/*")
    @Operation(summary = "", hidden = true)
    public void getInvalidResponseType() throws StatusCodeException {
        throw new StatusCodeException(HttpServletResponse.SC_NOT_ACCEPTABLE, MatrixErrorCodes.UNSUPPORTED_EXPORT_FORMAT, "This response format is not supported");
    }

    // Functional request methods

    @PostMapping(value = "/{profile}", produces = {"application/json;charset=UTF-8"})
    @RouterOperation(operation = @Operation(description = "Returns duration or distance matrix for multiple source and destination points.\n" +
            "By default a square duration matrix is returned where every point in locations is paired with each other. The result is null if a value can’t be determined.",
            summary = "Matrix Service"),
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json;charset=UTF-8")
    @ApiResponse(responseCode = "200",
            description = "Standard response for successfully processed requests. Returns JSON.",
            content = {@Content(
                    mediaType = "application/json;charset=UTF-8",
                    array = @ArraySchema(schema = @Schema(implementation = JSONMatrixResponse.class))
            )
            })
    public JSONMatrixResponse getDefault(@Parameter(name = "profile", description = "Specifies the matrix profile.", required = true, example = "driving-car") @PathVariable APIEnums.Profile profile,
                                         @Parameter(description = "The request payload", required = true) @RequestBody MatrixRequest request) throws StatusCodeException {
        return getJsonMime(profile, request);
    }

    @PostMapping(value = "/{profile}/json", produces = {"application/json;charset=UTF-8"})
    @RouterOperation(operation = @Operation(
            summary = "Get a matrix calculation from the specified profile",hidden = true),
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json;charset=UTF-8")
    @ApiResponse(responseCode = "200",
            description = "Standard response for successfully processed requests. Returns JSON.",
            content = {@Content(
                    mediaType = "application/json;charset=UTF-8",
                    array = @ArraySchema(schema = @Schema(implementation = JSONMatrixResponse.class))
            )
            })
    public JSONMatrixResponse getJsonMime(
            @Parameter(description = "Specifies the matrix profile.", required = true, example = "driving-car") @PathVariable APIEnums.Profile profile,
            @Parameter(description = "The request payload", required = true) @RequestBody MatrixRequest originalRequest) throws StatusCodeException {
        originalRequest.setProfile(profile);
        originalRequest.setResponseType(APIEnums.MatrixResponseType.JSON);
        MatrixResult matrixResult = originalRequest.generateMatrixFromRequest();

        return new JSONMatrixResponse(matrixResult, originalRequest);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(final MissingServletRequestParameterException e) {
        return errorHandler.handleStatusCodeException(new MissingParameterException(MatrixErrorCodes.MISSING_PARAMETER, e.getParameterName()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConversionFailedException.class, HttpMessageConversionException.class, Exception.class})
    public ResponseEntity<Object> handleReadingBodyException(final Exception e) {
        final Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException) {
            return errorHandler.handleUnknownParameterException(new UnknownParameterException(MatrixErrorCodes.UNKNOWN_PARAMETER, ((UnrecognizedPropertyException) cause).getPropertyName()));
        } else if (cause instanceof InvalidFormatException) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_FORMAT, "" + ((InvalidFormatException) cause).getValue()));
        } else if (cause instanceof ConversionFailedException) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "" + ((ConversionFailedException) cause).getValue()));
        } else if (cause instanceof InvalidDefinitionException) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, ((InvalidDefinitionException) cause).getPath().get(0).getFieldName()));
        } else if (cause instanceof MismatchedInputException) {
            return errorHandler.handleStatusCodeException(new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_FORMAT, ((MismatchedInputException) cause).getPath().get(0).getFieldName()));
        } else {
            // Check if we are missing the body as a whole
            if (e.getLocalizedMessage().startsWith("Required request body is missing")) {
                return errorHandler.handleStatusCodeException(new EmptyElementException(MatrixErrorCodes.MISSING_PARAMETER, "Request body could not be read"));
            }
            return errorHandler.handleGenericException(e);
        }
    }

    @ExceptionHandler(StatusCodeException.class)
    public ResponseEntity<Object> handleException(final StatusCodeException e) {
        return errorHandler.handleStatusCodeException(e);
    }
}