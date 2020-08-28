package com.hadroncfy.sreplay.recording.mcpr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.util.PacketByteBuf;

public class SReplayFile implements IReplayFile {
    private static final String RECORDING_FILE = "recording.tmcpr";
    private static final String RECORDING_FILE_CRC32 = "recording.tmcpr.crc32";
    private static final String MARKER_FILE = "markers.json";
    private static final String META_FILE = "metaData.json";

    private static final Gson MARKER_GSON = new GsonBuilder()
        .registerTypeAdapter(Marker.class, new Marker.Serializer())
        .create();

    private static final Gson META_GSON = new GsonBuilder()
        .registerTypeAdapter(UUID.class, new UUIDSerializer())
        .create();

    private final File tmpDir;
    private final DataOutputStream packetStream;
    private final CRC32 crc32 = new CRC32();
    private long size = 0;

    private final File packetFile;
    private final File markerFile;
    private final File metaFile;

    public SReplayFile(File name) throws IOException {
        this.tmpDir = new File(name.getParentFile(), name.getName() + ".tmp");
        if (tmpDir.exists()) {
            throw new IOException("recording file " + name.toString() + " already exists!");
        } else if (!tmpDir.mkdirs()) {
            throw new IOException("Failed to create temp directory for recording " + tmpDir.toString());
        }

        packetFile = new File(tmpDir, RECORDING_FILE);
        markerFile = new File(tmpDir, MARKER_FILE);
        metaFile = new File(tmpDir, META_FILE);

        packetStream = new DataOutputStream(
                new DigestOutputStream(new BufferedOutputStream(new FileOutputStream(packetFile)), crc32));
    }

    private byte[] getPacketBytes(Packet<?> packet, boolean isLogin) throws Exception {
        NetworkState nstate = isLogin ? NetworkState.LOGIN : NetworkState.PLAY;
        int packetID = nstate.getPacketId(NetworkSide.CLIENTBOUND, packet);
        ByteBuf bbuf = Unpooled.buffer();
        PacketByteBuf packetBuf = new PacketByteBuf(bbuf);
        packetBuf.writeVarInt(packetID);
        packet.write(packetBuf);

        bbuf.readerIndex(0);
        byte[] ret = new byte[bbuf.readableBytes()];
        bbuf.readBytes(ret);
        bbuf.release();
        return ret;
    }

    @Override
    public void saveMetaData(Metadata data) throws IOException {
        data.fileFormat = "MCPR";
        data.fileFormatVersion = Metadata.CURRENT_FILE_FORMAT_VERSION;
        data.protocol = SharedConstants.getGameVersion().getProtocolVersion();

        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(metaFile)), StandardCharsets.UTF_8)){
            writer.write(META_GSON.toJson(data));
        }
    }

    @Override
    public void saveMarkers(List<Marker> markers) throws IOException {
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(markerFile)), StandardCharsets.UTF_8)){
            writer.write(MARKER_GSON.toJson(markers));
        }
    }

    @Override
    public void savePacket(long timestamp, Packet<?> packet, boolean isLoginPhase) throws Exception {
        byte[] data = getPacketBytes(packet, isLoginPhase);
        packetStream.writeInt((int)timestamp);
        packetStream.writeInt(data.length);
        packetStream.write(data);
        size += data.length + 8;
    }

    @Override
    public long getRecordedBytes() {
        return size;
    }

    @Override
    public void closeAndSave(File file) throws IOException {
        packetStream.close();
        
        try (ZipOutputStream os = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))){
            for (String fileName: tmpDir.list()){
                os.putNextEntry(new ZipEntry(fileName));
                File f = new File(tmpDir, fileName);
                copy(new BufferedInputStream(new FileInputStream(f)), os);
            }
            
            os.putNextEntry(new ZipEntry(RECORDING_FILE_CRC32));
            Writer writer = new OutputStreamWriter(os);
            writer.write(Long.toString(crc32.getValue()));
            writer.flush();
        }

        for (String fileName: tmpDir.list()){
            File f = new File(tmpDir, fileName);
            Files.delete(f.toPath());
        }
        Files.delete(tmpDir.toPath());
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[2048];
        int len;
        while ((len = in.read(buffer)) > -1){
            out.write(buffer, 0, len);
        }
        in.close();
    }
}