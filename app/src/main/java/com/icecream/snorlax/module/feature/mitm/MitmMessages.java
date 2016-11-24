package com.icecream.snorlax.module.feature.mitm;

import com.google.protobuf.ByteString;

import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;

public class MitmMessages {
	public final RequestType requestType;
	public final ByteString request;
	public final ByteString response;

	public MitmMessages(final RequestType requestType, final ByteString request, final ByteString response) {
		this.requestType = requestType;
		this.request = request;
		this.response = response;
	}
}
