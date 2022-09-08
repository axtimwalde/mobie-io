package org.embl.mobie.io.openorganelle;

import java.util.Arrays;

import org.embl.mobie.io.n5.util.ArrayCreator;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;

public class OrganelleArrayCreator<A, T extends NativeType<T>> extends ArrayCreator {
    public OrganelleArrayCreator(CellGrid cellGrid, DataType dataType) {
        super(cellGrid, dataType);
    }

    public A createArray(DataBlock<?> dataBlock, long[] gridPosition) {
        long[] cellDims = getCellDims(gridPosition);
        int n = (int) (cellDims[0] * cellDims[1] * cellDims[2]);
        return (A) VolatileDoubleArray(dataBlock, cellDims, n);
    }

    @Override
    public long[] getCellDims(long[] gridPosition) {
        long[] cellMin = new long[3];
        int[] cellDims = new int[3];
        cellGrid.getCellDimensions(gridPosition, cellMin, cellDims);
        return Arrays.stream(cellDims).mapToLong(i -> i).toArray(); // casting to long for creating ArrayImgs.*
    }
}