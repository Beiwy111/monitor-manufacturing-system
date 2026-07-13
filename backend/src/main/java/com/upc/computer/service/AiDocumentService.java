package com.upc.computer.service;

import com.upc.computer.dto.AiDocumentConfirmRequest;
import com.upc.computer.dto.AiDocumentConfirmResponse;
import com.upc.computer.dto.AiDocumentParseResult;
import org.springframework.web.multipart.MultipartFile;

public interface AiDocumentService {

    AiDocumentParseResult parseDocument(MultipartFile file);

    AiDocumentConfirmResponse confirmDocument(AiDocumentConfirmRequest request);
}
