package com.socialportal.portal.service.impl;

import com.socialportal.portal.model.image.ImageData;
import com.socialportal.portal.model.issues.IssueImage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageServiceImplTest {

    private final ImageServiceImpl imageService = new ImageServiceImpl();

    @Test
    void buildImageCompressesAndCopiesMetadata() throws IOException {
        byte[] payload = "binary-payload".getBytes();
        var file = new MockMultipartFile("file", "pic.png", "image/png", payload);

        ImageData result = imageService.buildImage(file);

        assertEquals("pic.png", result.getName());
        assertEquals("image/png", result.getType());
        assertArrayEquals(payload, imageService.getPayload(result));
    }

    @Test
    void imageMapperCopiesAllFieldsToTargetImage() {
        var source = new ImageData(1L, "name", "type", new byte[]{1, 2, 3});
        var target = new IssueImage();

        imageService.imageMapper(source, target);

        assertEquals("name", target.getName());
        assertEquals("type", target.getType());
        assertArrayEquals(new byte[]{1, 2, 3}, target.getImageData());
    }
}
