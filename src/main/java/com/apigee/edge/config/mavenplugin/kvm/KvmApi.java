package com.apigee.edge.config.mavenplugin.kvm;

import com.apigee.edge.config.rest.RestUtil;
import com.google.api.client.http.HttpResponse;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;

public class KvmApi extends KvmOperations implements Kvm {

	@Override
    public HttpResponse getKvm(KvmValueObject kvmValueObject) throws IOException {
		RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
        return restUtil.getAPIConfig(kvmValueObject.getProfile(),
        		kvmValueObject.getApi(),
        		"keyvaluemaps/"+kvmValueObject.getKvmName());
    }
	
	@Override
    public HttpResponse getEntriesForKvm(KvmValueObject kvmValueObject, String kvmEntryName) throws IOException {
		RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
		return restUtil.getAPIConfig(kvmValueObject.getProfile(),
                kvmValueObject.getApi(),
                "keyvaluemaps",
                kvmValueObject.getKvmName(),
                "entries",
                kvmEntryName);
    }

    @Override
    public HttpResponse updateKvmEntries(KvmValueObject kvmValueObject, String kvmEntryName, String kvmEntryValue) throws IOException {
    	RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
    	return restUtil.updateAPIConfig(kvmValueObject.getProfile(),
                kvmValueObject.getApi(),
                "keyvaluemaps",
                kvmValueObject.getKvmName(),
                "entries",
                kvmEntryName,
                kvmEntryValue);
    }

    @Override
    public HttpResponse createKvmEntries(KvmValueObject kvmValueObject, String kvmEntryValue) throws IOException {
    	RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
        return restUtil.createAPIConfig(kvmValueObject.getProfile(),
                kvmValueObject.getApi(),
                "keyvaluemaps",
                kvmValueObject.getKvmName(),
                "entries",
                kvmEntryValue);
    }

    @Override
    public void update(KvmValueObject kvmValueObject)
            throws IOException, MojoFailureException {
        super.update(kvmValueObject);
    }
}
