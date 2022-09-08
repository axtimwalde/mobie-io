package org.embl.mobie.io.ome.zarr.openers;

import java.io.File;
import java.io.IOException;

import org.embl.mobie.io.n5.openers.BDVOpener;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.readers.N5OmeZarrReader;

import com.google.gson.GsonBuilder;

import bdv.util.volatiles.SharedQueue;
import mpicbg.spim.data.SpimData;
import net.imglib2.util.Cast;

public class OMEZarrOpener extends BDVOpener {
    private final String filePath;

    public OMEZarrOpener(String filePath) {
        this.filePath = filePath;
    }

    public static SpimData openFile(String filePath) throws IOException {
        OMEZarrOpener omeZarrOpener = new OMEZarrOpener(filePath);
        return omeZarrOpener.readFile();
    }

    public static SpimData openFile(String filePath, SharedQueue sharedQueue) throws IOException {
        N5OMEZarrImageLoader.logging = logging;
        OMEZarrOpener omeZarrOpener = new OMEZarrOpener(filePath);
        return omeZarrOpener.readFile(sharedQueue);
    }

    private SpimData readFile(SharedQueue sharedQueue) throws IOException {
        N5OMEZarrImageLoader.logging = logging;
        N5OmeZarrReader reader = new N5OmeZarrReader(this.filePath, new GsonBuilder());
        N5OMEZarrImageLoader imageLoader = new N5OMEZarrImageLoader(reader, sharedQueue);
        return new SpimData(
            new File(this.filePath),
            Cast.unchecked(imageLoader.getSequenceDescription()),
            imageLoader.getViewRegistrations());
    }

    private SpimData readFile() throws IOException {
        N5OMEZarrImageLoader.logging = logging;
        N5OmeZarrReader reader = new N5OmeZarrReader(this.filePath, new GsonBuilder());
        N5OMEZarrImageLoader imageLoader = new N5OMEZarrImageLoader(reader);
        return new SpimData(
            new File(this.filePath),
            Cast.unchecked(imageLoader.getSequenceDescription()),
            imageLoader.getViewRegistrations());
    }

}
