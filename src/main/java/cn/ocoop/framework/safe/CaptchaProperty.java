package cn.ocoop.framework.safe;

import com.google.common.collect.Maps;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.ChineseCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

import static com.wf.captcha.base.Captcha.TYPE_ONLY_NUMBER;

@RefreshScope
@Data
@ConfigurationProperties(prefix = CaptchaProperty.PREFIX)
public class CaptchaProperty {
    static final String PREFIX = "safe.captcha";
    private static Map<Class<? extends Captcha>, CaptchaSupplier> class_Strategy = Maps.newHashMap();

    static {
        class_Strategy.put(SpecCaptcha.class, property -> {
            SpecCaptcha specCaptcha = new SpecCaptcha(property.getWidth(), property.getHeight(), property.getLength());
            specCaptcha.setFont(property.getFont());
            return specCaptcha;
        });
        class_Strategy.put(GifCaptcha.class, property -> {
            GifCaptcha gifCaptcha = new GifCaptcha(property.getWidth(), property.getHeight(), property.getLength());
            gifCaptcha.setFont(property.getFont());
            return gifCaptcha;
        });
        class_Strategy.put(ChineseCaptcha.class, property -> {
            ChineseCaptcha chineseCaptcha = new ChineseCaptcha(property.getWidth(), property.getHeight(), property.getLength());
            chineseCaptcha.setFont(property.getFont());
            return chineseCaptcha;
        });
        class_Strategy.put(ArithmeticCaptcha.class, property -> {
            ArithmeticCaptcha arithmeticCaptcha = new ArithmeticCaptcha(property.getWidth(), property.getHeight(), property.getLength());
            arithmeticCaptcha.setFont(property.getFont());
            return arithmeticCaptcha;
        });
    }

    private Class<? extends Captcha> type = ArithmeticCaptcha.class;
    private int width = 130;
    private int height = 48;
    private int length = 4;
    private int mixMode = TYPE_ONLY_NUMBER;
    private int font = Captcha.FONT_1;

    public Captcha getCaptcha() throws IOException, FontFormatException {
        return class_Strategy.get(getType()).supplier(this);
    }

    @FunctionalInterface
    interface CaptchaSupplier {
        Captcha supplier(CaptchaProperty property) throws IOException, FontFormatException;
    }


}
