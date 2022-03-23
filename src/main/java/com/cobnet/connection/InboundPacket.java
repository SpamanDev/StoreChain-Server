package com.cobnet.connection;

import com.cobnet.common.Endian;
import com.cobnet.interfaces.connection.InputTransmission;
import com.cobnet.spring.boot.controller.handler.InboundOperation;
import com.cobnet.spring.boot.core.ProjectBeanHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.Map;

public class InboundPacket extends Packet implements InputTransmission<byte[]> {

	private ByteBuf byteBuf;
	
	private long opcode;

	public InboundPacket(NettyServer<?> server) {

		this(new byte[]{}, server);
	}
	
	public InboundPacket(byte[] data, NettyServer<?> server) {

		this(Unpooled.copiedBuffer(data), server);
	}
	
	
	public InboundPacket(ByteBuf buf, NettyServer<?> server) {

		super(buf.array(), server);

		this.byteBuf = buf.copy();

		if(buf.readableBytes() >= 4) {

			this.decodeHeader();
		}

	}

    
    public InboundOperation getOperation() {

    	return InboundOperation.getByCode(opcode);
    }
    
    public long getOpcode() {

		return this.opcode;
    }
    
    public void decodeHeader() {
    	
    	if(this.byteBuf.array().length < 4) {
    		
    		throw new IllegalArgumentException("Header size is less then 32 bits");
    	}
    	
    	this.opcode = decodeUInt();
    }
    
    public byte[] decode(long length) {
    	
    	
    	ByteBuf buf = null;
    	
    	if(length > Integer.MAX_VALUE) {
    		
    		buf = Unpooled.buffer();

    	} else {
    		
    		buf = Unpooled.buffer((int)length);
    	}
    	
    	long left = length;
		
    	for(int i = 0; i < Math.ceil(length / (double)Integer.MAX_VALUE); i++) {
    		
    		buf.writeBytes(byteBuf.readBytes(left > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)left));
    		
    		left -= left > Integer.MAX_VALUE ? Integer.MAX_VALUE : left;
    	}
    	
    	return buf.array();
    }
    
    public byte decodeByte() {

		return byteBuf.readByte();
    }
    
    public short decodeUByte() {

		return byteBuf.readUnsignedByte();
    }
    
    public short decodeShort() {

    	if(this.getServer().getEndian() == Endian.BIG) {

    		return byteBuf.readShort();
    	}
    	
    	return byteBuf.readShortLE();
    }
    
    public int decodeUShort() {

    	if(this.getServer().getEndian() == Endian.BIG) {

    		return byteBuf.readUnsignedShort();
    	}
    	
    	return byteBuf.readUnsignedShortLE();
    }
    
    
    public int decodeInt() {

    	if(this.getServer().getEndian() == Endian.BIG) {

    		return byteBuf.readInt();
    	}
    	
    	return byteBuf.readIntLE();
    }
    
    public long decodeUInt() {

    	if(this.getServer().getEndian() == Endian.BIG) {

    		return byteBuf.readUnsignedInt();
    	}
    	
    	return byteBuf.readUnsignedIntLE();
    }
    
    public long decodeLong() {

    	if(this.getServer().getEndian() == Endian.BIG) {

    		return byteBuf.readLong();
    	}
    	
    	return byteBuf.readLongLE();
    }
    
    public float decodeFloat() {

    	if(this.getServer().getEndian() == Endian.BIG) {

    		return byteBuf.readFloat();
    	}
    	
    	return byteBuf.readFloatLE();
    }
    
    public double decodeDouble() {

    	if(this.getServer().getEndian() == Endian.BIG) {

    		return byteBuf.readDouble();
    	}
    	
    	return byteBuf.readDoubleLE();
    }
    
    public String decodeString() {

    	long size = this.decodeUInt();

    	return new String(this.decode(size), this.getServer().getCharset());
    }
    
    public String decodeText() {

    	long size = this.decodeLong();

    	return new String(this.decode(size), this.getServer().getCharset());
    }
    
	public Map<?,?> decodeMap() throws JsonMappingException, JsonProcessingException {

    	return ProjectBeanHolder.getObjectMapper().readValue(decodeString(), Map.class);
    }
    
	public Map<?,?> decodeBigMap() throws JsonMappingException, JsonProcessingException {

    	return ProjectBeanHolder.getObjectMapper().readValue(decodeText(), Map.class);
    }

    @Override
    public int getLength() {

		return byteBuf.readableBytes();
    }
	
    @Override
    public byte[] getData() {

		return byteBuf.array();
    }
    
    @Override
    public void setData(byte[] buf) {

    	dispose();
    	super.setData(buf);
    	byteBuf = Unpooled.copiedBuffer(buf);
    }
    
	@Override
	public ByteBuf getBuf() {

		return byteBuf.asReadOnly();
	}

	@Override
	public InboundPacket clone() throws CloneNotSupportedException {

		return new InboundPacket(byteBuf, this.getServer());
	}

	@Override
	public void dispose() {

		byteBuf.release();
	}
}
