package com.mediaconverter.config;


import com.mediaconverter.exception.LogicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

@ControllerAdvice
public class ExceptionHandlerAdvice {

    protected final Logger logger = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);



    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> duplicated(DataIntegrityViolationException ex) {
        logger.error("违反数据完整性", ex);
        return ResponseEntity.badRequest().body("违反数据完整性");
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.error(ex.getMessage(),ex);
        BindingResult bindingResult = ex.getBindingResult();
        StringBuilder errorMesssage = new StringBuilder("格式有误: \n");

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMesssage.append(fieldError.getDefaultMessage()).append("\n");
        }
        return ResponseEntity.badRequest().body(errorMesssage.toString());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> validateError(HttpMessageNotReadableException ex) {
        logger.error(ex.getMessage(),ex);
        return ResponseEntity.badRequest().body("数据格式有误");
    }


    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody
    ResponseEntity<String> dbError(DataAccessException ex) {
        logger.error("未知错误", ex);
        return ResponseEntity.badRequest().body("无权访问");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> fileOverSize(MaxUploadSizeExceededException ex) {
        logger.error(ex.getMessage(),ex);
        return ResponseEntity.badRequest().body("文件过大");
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody
    ResponseEntity<String> ioError(IOException ex) {
        logger.error(ex.getMessage(),ex);
        return ResponseEntity.badRequest().body("IO错误");
    }

    @ExceptionHandler({LogicException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> logicException(Exception ex) {
        logger.error(ex.getMessage(),ex);
        return ResponseEntity.badRequest().body(ex.getMessage());
    }



    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> mediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        logger.error("捕获异常：" + e.getMessage(), e);
        return ResponseEntity.badRequest().body("媒体格式错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody
    ResponseEntity<String> lastDefence(Exception ex) {
        logger.error("lastDefence:", ex);
        return ResponseEntity.internalServerError().body("未知错误");
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> requestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        logger.error(e.getMessage(),e);
        return ResponseEntity.badRequest().body("不支持请求方法");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody
    ResponseEntity<String> missingServletRequestParameter(MissingServletRequestParameterException e) {
        logger.error(e.getMessage(),e);
        return ResponseEntity.badRequest().body("缺少请求参数");
    }


}
