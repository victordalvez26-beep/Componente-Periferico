package uy.edu.tse.hcen.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HcenCentralUrlUtilTest {

    @Test
    void testBuildApiUrlWithPath() {
        String path = "/config/test";
        String url = HcenCentralUrlUtil.buildApiUrl(path);
        
        assertNotNull(url);
        assertTrue(url.contains("/config/test"));
    }
    
    @Test
    void testBuildApiUrlWithoutLeadingSlash() {
        String path = "usuarios/list";
        String url = HcenCentralUrlUtil.buildApiUrl(path);
        
        assertNotNull(url);
        assertTrue(url.contains("usuarios/list"));
    }
    
    @Test
    void testBuildApiUrlWithEmptyPath() {
        String url = HcenCentralUrlUtil.buildApiUrl("");
        assertNotNull(url);
    }
}

