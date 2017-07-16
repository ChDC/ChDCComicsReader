package com.chdc.comicsreader.fs;

import com.chdc.comicsreader.book.Page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Created by Wen on 2017/7/6.
 */

public class File implements Serializable{


    public interface Visitor {
        boolean visit(File file);
        Object getResult();
    }

    public static final Pattern IMAGE_FILE_PATTERN = Pattern.compile(".*\\.(jpg|png|bmp|jpeg|gif)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern DELETE_FILE_PATTERN = Pattern.compile(".*\\.(jpg|png|bmp|jpeg|gif)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern BLOCK_DIRECTORY_PATTERN = Pattern.compile("^$", Pattern.CASE_INSENSITIVE);
    public static final Pattern ARCHIVE_FILE_PATTERN = Pattern.compile(".*\\.(rar)$", Pattern.CASE_INSENSITIVE);

    protected File parent;
    protected List<File> children;
    protected boolean cacheParent = false;
    protected boolean cacheChildren = false;

    protected String name;
    protected String url;
    protected FileImplement fileImplement;

    public File(String url){
        this.url = url;
    }

    protected FileImplement getFileImplement() {
        if(fileImplement == null)
            fileImplement = FileImplement.getFileImplementByURLType(this.url);
        return fileImplement;
    }

    public void setFileImplement(FileImplement fileImplement){
        this.fileImplement = fileImplement;
    }

    public boolean delete() {
        // 只删除目录中的图片，如果删除后目录为空则删除空目录
        return this.getFileImplement().delete(url, DELETE_FILE_PATTERN, false, true);
    }

    public boolean isCacheParent() {
        return cacheParent;
    }

    public void setCacheParent(boolean cacheParent) {
        this.cacheParent = cacheParent;
    }

    public boolean isCacheChildren() {
        return cacheChildren;
    }

    public void setCacheChildren(boolean cacheChildren) {
        this.cacheChildren = cacheChildren;
    }

    public String getName() {
        if(name == null)
            name = getFileImplement().getName(url);
        return name;
    }


    public String getURL() {
        return url;
    }

    public File getParent(){
        if(parent != null)
            return parent;
        File p = new File(this.getFileImplement().getParent(url));
        if(cacheParent)
            parent = p;
        return p;
    }

    public List<File> getChildren(){
        if(children != null)
            return children;
        if(url == null)
            return null;

        String[][] result = this.getFileImplement().getChildren(url, IMAGE_FILE_PATTERN, BLOCK_DIRECTORY_PATTERN, ARCHIVE_FILE_PATTERN);
        List<Page> files = new ArrayList<>(result[0].length);
        for(String s: result[0]) {
            Page page = new Page(s);
            page.parent = this;
            page.setPageType(Page.PageType.NotEnd);
            files.add(page);
        }

        List<File> dirs = new ArrayList<>(result[1].length + result[2].length);
        for(String s : result[1]){
            File file = new File(s);
            file.parent = this;
            dirs.add(file);
        }
        for(String s : result[2]){
            ArchiveBridgeFile file = ArchiveBridgeFile.getArchiveBridgeFile(s);
            file.parent = this;
            dirs.add(file);
        }

        // TODO: 汉语排序，如汉语中的 章节一，章节二，
        // TODO: 数字序号排序，如 001 排在 02 前面
        Collections.sort(files, ((f1, f2) -> String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName())));
        Collections.sort(dirs, (f1, f2) -> String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName()));

        // 设置 Page 的头尾，必须在 Sort 之后设置
        if(files.size() > 0){
            files.get(files.size() - 1).setPageType(Page.PageType.TailEnd);
            files.get(0).setPageType(Page.PageType.HeadEnd);
        }

        List<File> cs = new ArrayList<>(files.size() + dirs.size());
        cs.addAll(files);
        cs.addAll(dirs);
        if(cacheChildren)
            children = cs;
        return cs;
    }

    /**
     * 获取兄弟元素
     * @param offset
     * @return
     */
    public File getSibling(int offset){
        File parent = this.getParent();
        if(parent == null)
            return null;
        List<File> children = parent.getChildren();
        if(children == null)
            return null;
        int i = children.indexOf(this);
        if(i < 0) return null;

        int size = children.size();
        for(int ni = i + offset; ni >=0 && ni < size; ni += offset) {
            File s = children.get(ni);
            if(s.isValid())
                return s;
        }
        return null;
    }

    /**
     * 先根遍历
     * @param visitor
     * @param reversed
     */
    protected boolean preOrderTraverseFile(Visitor visitor, boolean reversed){
        if(visitor == null)
            return true;
        List<File> children = this.getChildren();
        if(children == null)
            return true;
        if(!reversed) {
            int childrenSize = children.size();
            for (int i = 0; i < childrenSize; i++) {
                File c = children.get(i);
                if(c == null)
                    continue;
                if (!visitor.visit(c) || !c.preOrderTraverseFile(visitor, reversed))
                    return false;
            }
        }
        else
            for(int i = children.size() - 1; i >= 0; i--){
                File c = children.get(i);
                if(c == null)
                    continue;
                if (!visitor.visit(c) || !c.preOrderTraverseFile(visitor, reversed))
                    return false;
            }
        return true;
    }

    /**
     * 获取第一页
     * @return
     */
    public Page getHeadEndPage(){
        return this.getEndPage(false);
    }

    /**
     * 获取最后一页
     * @return
     */
    public Page getTailEndPage(){
        return this.getEndPage(true);
    }

    protected Page getEndPage(boolean reversed){
        if(this instanceof Page)
            return (Page)this;

        Visitor visitor = new Visitor() {

            Page page;
            @Override
            public boolean visit(File file) {
                if(file instanceof Page && file.isValid()) {
                    page = (Page)file;
                    if(reversed && page.getPageType() != Page.PageType.HeadEnd)
                        page.setPageType(Page.PageType.TailEnd);
                    else
                        page.setPageType(Page.PageType.HeadEnd);
                    return false;
                }
                return true;
            }

            @Override
            public Object getResult() {
                return page;
            }
        };
        this.preOrderTraverseFile(visitor, reversed);
        return (Page)visitor.getResult();
    }

    public static Page getFirstPageFromFile(String url){
        String name = FileImplement.getFileImplementByURLType(url).getName(url);
        if(IMAGE_FILE_PATTERN.matcher(name).find())
            return new Page(url);
        else if(ARCHIVE_FILE_PATTERN.matcher(name).find()){
            ArchiveBridgeFile file = ArchiveBridgeFile.getArchiveBridgeFile(url);
            return file.getHeadEndPage();
        }
        return null;
    }

    public boolean isValid(){
        return this.getFileImplement().exists(url);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof File && Objects.equals(((File) obj).url, this.url);
    }

    @Override
    public int hashCode(){
        return url == null ? 0 : this.url.hashCode();
    }
}
