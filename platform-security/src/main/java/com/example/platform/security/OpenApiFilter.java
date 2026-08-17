package com.example.platform.security;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.PathItem;

public class OpenApiFilter implements OASFilter {

    @Override
    public PathItem filterPathItem(PathItem pathItem) {
        // 在產生 Swagger 文件時，將 POST 以外的方法清空（不顯示）
        pathItem.setGET(null);
        pathItem.setDELETE(null);
        pathItem.setPUT(null);
        pathItem.setPATCH(null);
        pathItem.setHEAD(null);
        return pathItem;
    }
}