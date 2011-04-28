package com.rackspace.plugins;

import com.rackspace.api.clients.veracode.DefaultVeracodeApiClient;
import com.rackspace.api.clients.veracode.VeracodeApiClient;
import com.rackspace.api.clients.veracode.VeracodeApiException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: john.madrid
 * Date: 4/22/11
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class VeracodeNotifier extends Notifier {

    private final String includes;
    private final String username;
    private final String password;
    private final String applicationName;
    private final int addToBuildNumber;

    @DataBoundConstructor
    public VeracodeNotifier(String includes, String username, String password, String applicationName, String addToBuildNumber) {
        this.includes = includes;
        this.username = username;
        this.password = password;
        this.applicationName = applicationName;
        this.addToBuildNumber = Integer.valueOf(addToBuildNumber);
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        FilePath workspace = build.getWorkspace();

        FilePath[] filesToScan = workspace.list(includes);
        listener.getLogger().println("Uploading Files to Veracode: " + Arrays.toString(filesToScan));
        listener.getLogger().println("Base URI" + getDescriptor().getEndpoint());

        VeracodeApiClient client = new DefaultVeracodeApiClient(getDescriptor().getEndpoint(), username, password, listener.getLogger());

        String buildId = null;

        try{
            buildId = client.scanArtifacts(convertFilePaths(filesToScan), build.getNumber() + addToBuildNumber, applicationName);

            listener.getLogger().println("Veracode Scan Succeeded. Build ID: " + buildId);
        } catch(VeracodeApiException e){
            throw new RuntimeException("Veracode Scan Failed", e);
        } finally {
            client.shutdown();
        }

        return true;
    }

    public String getIncludes() {
        return includes;
    }

    private List<File> convertFilePaths(FilePath[] filePaths){
        List<File> files = new ArrayList<File>();

        for(FilePath path : filePaths){
            files.add(getFile(path));
        }

        return files;
    }

    private File getFile(FilePath path) {
        File file;

        try {
            file = path.act(new FilePath.FileCallable<File>() {
                public File invoke(File f, VirtualChannel channel) {
                    return f;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Could not load the build artifact to send.", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("The file operation was interrupted.", e);
        }

        return file;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String endpoint;

        public DescriptorImpl() {
            super(VeracodeNotifier.class);
            endpoint = defaultEndpoint();
        }

        @Override
        public String getDisplayName() {
            return "Publish Artifacts For Veracode Scan";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> item) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {

            // to persist global configuration information,
            // set that to properties and call save().
            endpoint = o.getString("endpoint");
            save();
            return super.configure(req, o);
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String defaultEndpoint(){
            return "https://analysiscenter.veracode.com/api/";
        }
    }

}
