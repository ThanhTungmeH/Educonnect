package org.example.educonnect1.client.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.util.Map;

public class CloudinaryService {  // đổi tên class ở đây
    private final Cloudinary cloudinary;
    public CloudinaryService() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "do46eak3c",
                "api_key", "769755297178562",
                "api_secret", "TBsrXG2wfv4Fji4L2wM-xhenSXQ"
        ));
    }
    public String uploadFile(File file) throws Exception {
        Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
        return (String) uploadResult.get("secure_url");
    }
}
