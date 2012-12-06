package com.axelor.db;

/**
 * Initial implementation of a custom type for binary fields.
 * 
 */
public class Binary {

	private byte[] data;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
