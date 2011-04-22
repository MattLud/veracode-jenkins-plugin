package com.rackspace.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: john.madrid
 * Date: 4/22/11
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class VeracodeNotifier extends Notifier {

    private final String includes;
    private final String excludes;

    @DataBoundConstructor
    public VeracodeNotifier(String includes, String excludes) {
        this.includes = includes;
        this.excludes = excludes;
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
        listener.getLogger().println("Includes: " + includes);
        listener.getLogger().println("Excludes: " + excludes);
        listener.getLogger().println("Endpoint: " + getDescriptor().getEndpoint());

        return true;
    }

    public String getIncludes() {
        return includes;
    }

    public String getExcludes() {
        return excludes;
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
    }

}
