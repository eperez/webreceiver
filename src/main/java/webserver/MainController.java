package webserver;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;

@Controller
public class MainController {

    private boolean isSaved(final String hash) throws Exception {
        final File[] files = new File(".").listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.getName().contains(hash)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getSha256(final InputStreamSource inputStreamSource) throws Exception {
        final InputStream is = inputStreamSource.getInputStream();
        try {
             return org.apache.commons.codec.digest.DigestUtils.sha256Hex(is);
        } finally {
            try { is.close(); } catch (Throwable ignore) { }
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home() throws Exception {
        return new ModelAndView("index");
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ModelAndView post(
            @RequestParam(value = "file", required = false) final MultipartFile[] multipartFiles,
            @RequestParam(value = "text", required = false) final String text,
            final RedirectAttributes redirectAttributes
            ) throws Exception {

        if (multipartFiles != null) {
            for (final MultipartFile multipartFile : multipartFiles) {
                if (multipartFile != null && !multipartFile.isEmpty()) {
                    final String sha256Hex = getSha256(multipartFile);
                    if (!isSaved(sha256Hex)) {
                        final String originalFilename = multipartFile.getOriginalFilename();
                        final String filename = sha256Hex + (StringUtils.isNotBlank(originalFilename) ? "_" + originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_") : "");
                        final File file = new File(Paths.get("").toAbsolutePath().toFile(), filename);
                        multipartFile.transferTo(file);
                        redirectAttributes.addFlashAttribute("message",
                                originalFilename + " successfully uploaded!");
                    }
                }
            }
        }

        if (StringUtils.isNotEmpty(text)) {
            final String sha256Hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(text);
            if (!isSaved(sha256Hex)) {
                final java.io.Writer writer = new java.io.FileWriter(sha256Hex + ".txt", true);
                try {
                    writer.append(text);
                    writer.close();
                } finally {
                    try { writer.close(); } catch (Throwable ignore) { }
                }
            }
        }

        return new ModelAndView("redirect:/");
    }

}
