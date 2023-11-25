package com.ghostchu.mcplugins.barbatosstorybook;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoryBook {
    private final long time;
    private String author;
    private String title;
    private List<String> pages;
    private ItemStack itemStack;

    public StoryBook(String author, String title, List<String> pages, ItemStack itemStack, long time) {
        this.author = author;
        this.title = title;
        this.pages = pages;
        this.itemStack = itemStack;
        this.time = time;
    }

    public Optional<String> getAuthor() {
        return Optional.ofNullable(author);
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public List<String> getPages() {
        return pages;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public long getTime() {
        return time;
    }

    public void writeTo(File dataFolder) throws IOException {
        if (title == null || title.isBlank()) {
            title = "no title";
        }
        if (author == null || author.isBlank()) {
            author = "no author";
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("time", System.currentTimeMillis());
        yaml.set("book.author", author);
        yaml.set("book.title", title);
        yaml.set("book.content", pages);
        yaml.set("backup", itemStack);
        String str = yaml.saveToString();
        byte[] content = str.getBytes(StandardCharsets.UTF_8);
        tryWrite(dataFolder, author, content);
    }


    private void tryWrite(File dataFolder, String fileName, byte[] content) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        File file = new File(dataFolder, format.format(new Date()) + "-" + fileName + "-" + UUID.randomUUID() + ".yml");
        file.getParentFile().mkdirs();
        if (!file.createNewFile()) {
            return;
        }
        Files.write(file.toPath(), content);
    }

    public String checkFileName(String fileName) {
        Pattern pattern = Pattern.compile("[\\s\\\\/:*?\"<>|.]");
        Matcher matcher = pattern.matcher(fileName);
        fileName = matcher.replaceAll(""); // 将匹配到的非法字符以空替换
        return fileName;
    }
}
