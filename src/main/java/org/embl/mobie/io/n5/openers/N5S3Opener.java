package org.embl.mobie.io.n5.openers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.embl.mobie.io.n5.loaders.N5S3ImageLoader;
import org.embl.mobie.io.util.IOHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import bdv.util.volatiles.SharedQueue;
import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Cast;

@Slf4j
public class N5S3Opener extends S3Opener {
    public static final String SERVICE_ENDPOINT = "ServiceEndpoint";
    public static final String SIGNING_REGION = "SigningRegion";
    public static final String BUCKET_NAME = "BucketName";
    public static final String KEY = "Key";

    public N5S3Opener(String url) {
        super(url);
    }

    public static SpimData readURL(String url, SharedQueue sharedQueue) throws IOException {
        final N5S3Opener reader = new N5S3Opener(url);
        return reader.readURLData(url, sharedQueue);
    }

    private static TimePoints createTimepointsFromXml(final Element sequenceDescription) {
        final Element timepoints = sequenceDescription.getChild("Timepoints");
        final String type = timepoints.getAttributeValue("type");
        if (type.equals("range")) {
            final int first = Integer.parseInt(timepoints.getChildText("first"));
            final int last = Integer.parseInt(timepoints.getChildText("last"));
            final ArrayList<TimePoint> tps = new ArrayList<>();
            for (int i = first, t = 0; i <= last; ++i, ++t)
                tps.add(new TimePoint(t));
            return new TimePoints(tps);
        } else {
            throw new RuntimeException("unknown <Timepoints> type: " + type);
        }
    }

    private static Map<Integer, ViewSetup> createViewSetupsFromXml(final Element sequenceDescription) {
        final HashMap<Integer, ViewSetup> setups = new HashMap<>();
        final HashMap<Integer, Channel> channels = new HashMap<>();
        Element viewSetups = sequenceDescription.getChild("ViewSetups");

        for (final Element elem : viewSetups.getChildren("ViewSetup")) {
            final int id = XmlHelpers.getInt(elem, "id");
            int angleId = 0;
            Angle angle = new Angle(angleId);
            Channel channel = new Channel(angleId);
            Illumination illumination = new Illumination(angleId);
            try {
                final int channelId = XmlHelpers.getInt(elem, "channel");
                channel = channels.get(channelId);
                if (channel == null) {
                    channel = new Channel(channelId);
                    channels.put(channelId, channel);
                }
            } catch (NumberFormatException e) {
                if (logging) {
                    log.warn("No channel specified");
                }
            }
            try {
                final String sizeString = elem.getChildText("size");
                final String name = elem.getChildText("name");
                final String[] values = sizeString.split(" ");
                final Dimensions size = new FinalDimensions(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]));
                final String[] voxelValues = elem.getChild("voxelSize").getChildText("size").split(" ");
                final String unit = elem.getChild("voxelSize").getChildText("unit");
                final VoxelDimensions voxelSize = new FinalVoxelDimensions(unit,
                    Double.parseDouble(voxelValues[0]),
                    Double.parseDouble(voxelValues[1]),
                    Double.parseDouble(voxelValues[2]));
                final ViewSetup setup = new ViewSetup(id, name, size, voxelSize, channel, angle, illumination);
                setups.put(id, setup);
            } catch (Exception e) {
                log.warn("No pixel parameters specified");
            }
        }
        return setups;
    }

    public SpimData readURLData(String url, SharedQueue sharedQueue) throws IOException {
        InputStream stream = IOHelper.getInputStream(url);
        final SAXBuilder sax = new SAXBuilder();
        try {
            Document doc = sax.build(stream);
            final Element root = doc.getRootElement();
            final Element sequenceDescriptionElement = root.getChild("SequenceDescription");
            final Element elem = sequenceDescriptionElement.getChild("ImageLoader");
            final String serviceEndpoint = XmlHelpers.getText(elem, SERVICE_ENDPOINT);
            final String signingRegion = XmlHelpers.getText(elem, SIGNING_REGION);
            final String bucketName = XmlHelpers.getText(elem, BUCKET_NAME);
            final String key = XmlHelpers.getText(elem, KEY);
            final TimePoints timepoints = createTimepointsFromXml(sequenceDescriptionElement);
            final Map<Integer, ViewSetup> setups = createViewSetupsFromXml(sequenceDescriptionElement);
            final MissingViews missingViews = null;
            final Element viewRegistrations = root.getChild("ViewRegistrations");
            final ArrayList<ViewRegistration> regs = new ArrayList<>();
            for (final Element vr : viewRegistrations.getChildren("ViewRegistration")) {
                final int timepointId = Integer.parseInt(vr.getAttributeValue("timepoint"));
                final int setupId = Integer.parseInt(vr.getAttributeValue("setup"));
                final AffineTransform3D transform = new AffineTransform3D();
                transform.set(XmlHelpers.getDoubleArray(vr.getChild("ViewTransform"), "affine"));
                regs.add(new ViewRegistration(timepointId, setupId, transform));
            }
            SequenceDescription sequenceDescription = new SequenceDescription(timepoints, setups, null, (MissingViews) missingViews);
            N5S3ImageLoader imageLoader = new N5S3ImageLoader(serviceEndpoint, signingRegion, bucketName, key, sequenceDescription, sharedQueue);
            sequenceDescription.setImgLoader(imageLoader);
            imageLoader.setViewRegistrations(new ViewRegistrations(regs));
            imageLoader.setSeq(sequenceDescription);
            return new SpimData(null, Cast.unchecked(imageLoader.getSequenceDescription()), imageLoader.getViewRegistrations());
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        return null;
    }
}
