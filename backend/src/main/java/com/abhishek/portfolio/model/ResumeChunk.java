package com.abhishek.portfolio.model;

import java.util.List;

public record ResumeChunk(
    String id,
    String category,
    List<String> tags,
    String content
) {}
