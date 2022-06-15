package com.axelor.common.http;

import com.axelor.common.StringUtils;
import com.google.common.base.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/** This class provides some methods to deal with Content-Disposition as defined in RFC 6266 */
public class ContentDisposition {

    /**
     * form-data content-disposition type
     */
    public static final String FORM_DATA = "form-data";

    /**
     * attachment content-disposition type
     */
    public static final String ATTACHMENT = "attachment";

    /**
     * inline content-disposition type
     */
    public static final String INLINE = "inline";

    private final String type;
    private final String name;
    private final String filename;

    public ContentDisposition(String type,String name, String filename) {
        this.type = type;
        this.name = name;
        this.filename = filename;
    }

    public String getType() {
        return type;
    }

    public String getFilename() {
        return filename;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentDisposition that = (ContentDisposition) o;
        return Objects.equal(type, that.type) && Objects.equal(name, that.name) && Objects.equal(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, name, filename);
    }

    /**
     * Return the header value for this content disposition as defined in RFC 6266.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type != null) {
            sb.append(this.type);
        }
        if (this.name != null) {
            sb.append("; name=\"");
            sb.append(this.name).append('\"');
        }
        if (this.filename != null) {
            if (StringUtils.isAscii(filename)) {
                sb.append("; filename=\"");
                sb.append(escape(this.filename)).append('\"');
            }
            else {
                sb.append("; filename*=UTF-8''");
                sb.append(encode(this.filename));
            }
        }
        return sb.toString();
    }

    /**
     * Escape text for `"` and `\` characters
     * @param text the text to escape
     * @return escaped text
     */
    private static String escape(String text) {
        if (text.indexOf('"') == -1 && text.indexOf('\\') == -1) {
            return text;
        }
        boolean escaped = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length() ; i++) {
            char c = text.charAt(i);
            if (!escaped && c == '"') {
                sb.append("\"");
            }
            else {
                sb.append(c);
            }
            escaped = (!escaped && c == '\\');
        }
        if (escaped) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Whatever is it a valid char as defined in RFC 5987
     * @param c the char to check
     * @return <code>true</code> if the char is valid, <code>false</code> otherwise.
     */
    private static boolean isValidChar(byte c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
                c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
    }

    /**
     * Encode text as defined in RFC 5987
     * @param text the text to encore
     * @return encoded text
     */
    private static String encode(String text) {
        StringBuilder sb = new StringBuilder();
        for (byte b : text.getBytes(UTF_8)) {
            if (isValidChar(b)) {
                sb.append((char) b);
            }
            else {
                // to hex digit
                sb.append('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                sb.append(hex1);
                sb.append(hex2);
            }
        }
        return sb.toString();
    }

    /**
     * Builder for a {@code ContentDisposition} of type attachment.
     */
    public static Builder attachment() {
        return new Builder(ATTACHMENT);
    }

    /**
     * Builder for a {@code ContentDisposition} of type form-data.
     */
    public static Builder formData() {
        return new Builder(FORM_DATA);
    }

    /**
     * Builder for a {@code ContentDisposition} of type inline.
     */
    public static Builder inline() {
        return new Builder(INLINE);
    }

    /**
     * Builder for {@code ContentDisposition}
     */
    public static class Builder {

        private final String type;
        private String name;
        private String filename;

        /**
         * Builder for the given content disposition type
         * @param type the content disposition type
         */
        public Builder(String type) {
            this.type = type;
        }

        /**
         * Set the name parameter.
         * @param name the name
         * @return the {@code Builder}
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the filename parameter
         * @param filename the filename
         * @return the {@code Builder}
         */
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Build the content disposition
         * @return the {@code ContentDisposition}
         */
        public ContentDisposition build() {
            return new ContentDisposition(this.type, this.name, this.filename);
        }
    }

}
