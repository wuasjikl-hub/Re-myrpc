import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NettyTest {
    @Test
    public void testCompress() throws IOException {
        //本质就是将buf作为输入,将结果输出到另一个字节数组中
        byte[] bytes = {12, 12, 13, 14, 15, 15};
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(bytes);
        gzipOutputStream.flush();
        gzipOutputStream.finish();
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        System.out.println(Arrays.toString(byteArray));
    }
    @Test
    public void testDeCompress() throws IOException {
        //本质就是将buf作为输入,将结果输出到另一个字节数组中
        byte[] bytes = {31, -117, 8, 0, 0, 0, 0, 0, 0, -1, -29, -31, -31, -27, -29, -25, 7, 0, 80, 120, 35, -20, 6, 0, 0, 0};
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        //GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);


        byte[] bytes1 = gzipInputStream.readAllBytes();
        //byte[] byteArray = byteArrayOutputStream.toByteArray();
        System.out.println(Arrays.toString(bytes1));
    }
}
