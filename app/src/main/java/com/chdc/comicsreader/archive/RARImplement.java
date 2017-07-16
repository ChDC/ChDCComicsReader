package com.chdc.comicsreader.archive;

import java.io.File;

/**
 * Created by Wen on 2017/7/15.
 */

public class RARImplement extends ArchiveImplement {

    public static final ArchiveImplement INSTANCE = new RARImplement();

    @Override
    public Archive createArchive(String file, String password)  throws PasswordIsWrongException {
        try {
            return new RARArchive(file, password);
        }
        catch (PasswordIsWrongException e){
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getName(String url) {
        return new File(url).getName();
    }
}
