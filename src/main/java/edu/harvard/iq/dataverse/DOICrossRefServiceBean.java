package edu.harvard.iq.dataverse;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.search.SearchConstants.PUBLIC;

@Stateless
public class DOICrossRefServiceBean extends AbstractGlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger(DOICrossRefServiceBean.class.getCanonicalName());

    @EJB
    DOICrossRefRegisterService doiCrossRefRegisterService;

    @Override
    public boolean alreadyExists(DvObject dvObject) throws Exception {
        if (dvObject == null) {
            logger.severe("Null DvObject sent to alreadyExists().");
            return false;
        }
        return alreadyExists(dvObject.getGlobalId());
    }

    @Override
    public boolean alreadyExists(GlobalId pid) throws Exception {
        logger.info("CrossRef alreadyExists");
        if (pid == null || pid.asString().isEmpty()) {
            logger.fine("No identifier sent.");
            return false;
        }
        boolean alreadyExists;
        String identifier = pid.asString();
        try {
            alreadyExists = doiCrossRefRegisterService.testDOIExists(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "alreadyExists failed");
            return false;
        }
        return alreadyExists;
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        logger.info("CrossRef createIdentifier");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getMetadataForCreateIndicator(dvObject);
        metadata.put("_status", "reserved");
        try {
            String retString = doiCrossRefRegisterService.reserveIdentifier(identifier, metadata, dvObject);
            logger.log(Level.FINE, "CrossRef create DOI identifier retString : " + retString);
            return retString;
        } catch (Exception e) {
            logger.log(Level.WARNING, "CrossRef Identifier not created: create failed", e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        logger.info("CrossRef getIdentifierMetadata");
        String identifier = getIdentifier(dvObject);
        HashMap<String, String> metadata = new HashMap<>();
        try {
            metadata = doiCrossRefRegisterService.getMetadata(identifier);
        } catch (Exception e) {
            logger.log(Level.WARNING, "getIdentifierMetadata failed", e);
        }
        return metadata;
    }

    @Override
    public Map<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String identifier) {
        logger.info("CrossRef lookupMetadataFromIdentifier");
        String identifierOut = getIdentifierForLookup(protocol, authority, identifier);
        HashMap<String, String> metadata = new HashMap<>();
        try {
            metadata = doiCrossRefRegisterService.getMetadata(identifierOut);
        } catch (Exception e) {
            logger.log(Level.WARNING, "None existing so we can use this identifier");
            logger.log(Level.WARNING, "identifier: {0}", identifierOut);
        }
        return metadata;
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        logger.info(" CrossRef modifyIdentifier");
        String identifier = getIdentifier(dvObject);
        try {
            HashMap<String, String> metadata = doiCrossRefRegisterService.getMetadata(identifier);
            doiCrossRefRegisterService.modifyIdentifier(identifier, metadata, dvObject);
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed", e);
            throw e;
        }
        return identifier;
    }

    @Override
    public boolean registerWhenPublished() {
        logger.info("CrossRef registerWhenPublished");
        return false;
    }

    @Override
    public List<String> getProviderInformation() {
        logger.info("CrossRef getProviderInformation");
        return null;
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {
        logger.info("CrossRef deleteIdentifier");
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.info("CrossRef updateIdentifierStatus");
        if(dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty() ){
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        Map<String, String> metadata = getUpdateMetadata(dvObject);
        metadata.put("_status", PUBLIC);
        metadata.put("datacite.publicationyear", generateYear(dvObject));
        metadata.put("_target", getTargetUrl(dvObject));
        try {
            doiCrossRefRegisterService.registerIdentifier(identifier, metadata, dvObject);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "modifyMetadata failed: " + e.getMessage(), e);
            return false;
        }
    }
}
