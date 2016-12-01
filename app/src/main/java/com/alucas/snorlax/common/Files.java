package com.alucas.snorlax.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import com.alucas.snorlax.module.util.Log;
import com.google.protobuf.ByteString;

import rx.Observable;

public class Files {
	public static Observable<ByteString> loadFileToByteString(final File file) {
		try (
			final FileInputStream fis = new FileInputStream(file);
			final FileChannel channel = fis.getChannel()
		) {
			final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).load();
			return Observable.just(ByteString.copyFrom(buffer));
		} catch (IOException e) {
			Log.e(e);
		}

		return Observable.empty();
	}
}
