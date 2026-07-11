package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.Result;
import com.upc.computer.dto.CustomerCreateOrderRequest;
import com.upc.computer.dto.CustomerFeedbackRequest;
import com.upc.computer.dto.CustomerProfileUpdateRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.AuthService;
import com.upc.computer.service.CustomerPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer/portal")
public class CustomerPortalController {

    @Autowired
    private CustomerPortalService customerPortalService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${file.upload.root-path:D:/upload/file/}")
    private String uploadRoot;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(customerPortalService.dashboard(requireSession(authorization)));
    }

    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> orders(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(customerPortalService.listOrders(requireSession(authorization)));
    }

    @GetMapping("/orders/{orderId}")
    public Result<Map<String, Object>> orderDetail(@PathVariable Long orderId,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(customerPortalService.getOrderDetail(requireSession(authorization), orderId));
    }

    @PostMapping("/orders")
    public Result<Map<String, Object>> createOrder(@RequestBody CustomerCreateOrderRequest request,
                                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success("提交成功", customerPortalService.createOrder(requireSession(authorization), request));
    }

    @GetMapping("/products")
    public Result<List<Map<String, Object>>> products() {
        return Result.success(customerPortalService.listProducts());
    }

    @GetMapping("/feedbacks")
    public Result<List<Map<String, Object>>> feedbacks(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(customerPortalService.listFeedbacks(requireSession(authorization)));
    }

    @PostMapping("/feedbacks")
    public Result<Map<String, Object>> submitFeedback(@RequestBody CustomerFeedbackRequest request,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(customerPortalService.submitFeedback(requireSession(authorization), request));
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(customerPortalService.getProfile(requireSession(authorization)));
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody CustomerProfileUpdateRequest request,
                                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(customerPortalService.updateProfile(requireSession(authorization), request));
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        String url = customerPortalService.saveUpload(requireSession(authorization), file);
        return Result.success(Map.of("url", url));
    }

    @GetMapping("/file/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        if (!StringUtils.hasText(filename) || filename.contains("..")) {
            throw new BusinessException("非法文件");
        }
        try {
            Path file = Paths.get(uploadRoot, "customer").resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                throw new BusinessException("文件不存在");
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文件读取失败");
        }
    }

    private LoginResponse requireSession(String authorization) {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "未登录或令牌无效");
        }
        LoginResponse session = authService.getLoginSession(token);
        if (session == null) {
            throw new BusinessException(401, "登录已失效，请重新登录");
        }
        return session;
    }
}
