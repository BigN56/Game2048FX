/*
 * Copyright (C) 2013-2015 2048FX 
 * Jose Pereda, Bruno Borges & Jens Deters
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpereda.game2048;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.StorageService;
import javafx.beans.property.StringProperty;

/**
 *
 * @author José Pereda
 */
public class SessionManager {

    public final String SESSION_PROPERTIES_FILENAME;
    private File path;
    private final Properties props = new Properties();
    private final GridOperator gridOperator;

    public SessionManager(GridOperator gridOperator) {
        this.gridOperator = gridOperator;
        path = Services.get(StorageService.class)
                .flatMap(service -> service.getPrivateStorage())
                .orElse(new File(System.getProperty("java.io.tmpdir")));
        this.SESSION_PROPERTIES_FILENAME = "game2048_" + gridOperator.getGridSize() + ".properties";
    }

    public void saveSession(Map<Location, Tile> gameGrid, Integer score, Long time) {
        try {
            for(int x=0; x<gridOperator.getGridSize(); x++){
                for(int y=0; y<gridOperator.getGridSize(); y++){
                    Tile t = gameGrid.get(new Location(x, y));
                    props.setProperty("Location_" + x + "_" + y,
                            t!=null? t.getValue().toString() : "0");
                }
            }
            props.setProperty("score", score.toString());
            props.setProperty("time", time.toString());
            File file=new File(path,SESSION_PROPERTIES_FILENAME);
            props.store(new FileWriter(file), SESSION_PROPERTIES_FILENAME);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int restoreSession(Map<Location, Tile> gameGrid, StringProperty time) {
        Reader reader = null;
        try {
            File file=new File(path,SESSION_PROPERTIES_FILENAME);
            reader = new FileReader(file);
            props.load(reader);
        } catch (FileNotFoundException ignored) {
            return -1;
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }

        for(int x=0; x<gridOperator.getGridSize(); x++){
            for(int y=0; y<gridOperator.getGridSize(); y++){
                String val = props.getProperty("Location_" + x + "_" + y);
                if (!val.equals("0")) {
                    Tile t = Tile.newTile(new Integer(val));
                    Location l = new Location(x, y);
                    t.setLocation(l);
                    gameGrid.put(l, t);
                }
            }
        }

        time.set(props.getProperty("time"));

        String score = props.getProperty("score");
        if (score != null) {
            return new Integer(score);
        }
        return 0;
    }

}
