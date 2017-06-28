package gov.samhsa.c2s.c2suiapi.service.phr;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import gov.samhsa.c2s.c2suiapi.infrastructure.phr.PhrUploadedDocumentsClient;
import gov.samhsa.c2s.c2suiapi.service.EnforceUserAuthForMrnService;
import gov.samhsa.c2s.c2suiapi.service.exception.phr.DocumentDeleteException;
import gov.samhsa.c2s.c2suiapi.service.exception.phr.DocumentInvalidException;
import gov.samhsa.c2s.c2suiapi.service.exception.phr.DocumentNameExistsException;
import gov.samhsa.c2s.c2suiapi.service.exception.phr.DocumentSaveException;
import gov.samhsa.c2s.c2suiapi.service.exception.phr.InvalidInputException;
import gov.samhsa.c2s.c2suiapi.service.exception.phr.NoDocumentsFoundException;
import gov.samhsa.c2s.c2suiapi.service.exception.phr.PhrClientInterfaceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
public class PhrUploadedDocumentsServiceImpl implements PhrUploadedDocumentsService {
    private final PhrUploadedDocumentsClient phrUploadedDocumentsClient;
    private final EnforceUserAuthForMrnService enforceUserAuthForMrnService;

    @Autowired
    public PhrUploadedDocumentsServiceImpl(PhrUploadedDocumentsClient phrUploadedDocumentsClient, EnforceUserAuthForMrnService enforceUserAuthForMrnService) {
        this.phrUploadedDocumentsClient = phrUploadedDocumentsClient;
        this.enforceUserAuthForMrnService = enforceUserAuthForMrnService;
    }


    @Override
    public List<Object> getAllDocumentTypeCodesList() {
        List<Object> documentTypeCodes;

        try {
            documentTypeCodes = phrUploadedDocumentsClient.getAllDocumentTypeCodesList();
        } catch (HystrixRuntimeException hystrixErr) {
            log.error("Unexpected instance of HystrixRuntimeException has occurred: ", hystrixErr);
            throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
        }

        return documentTypeCodes;
    }

    @Override
    public List<Object> getPatientDocumentInfoList(String patientMrn) {
        List<Object> uploadedDocuments;

        // Assert mrn belong to current user
        enforceUserAuthForMrnService.assertCurrentUserAuthorizedForMrn(patientMrn);

        try {
            uploadedDocuments = phrUploadedDocumentsClient.getPatientDocumentInfoList(patientMrn);
        } catch (HystrixRuntimeException hystrixErr) {
            Throwable causedBy = hystrixErr.getCause();

            if(!(causedBy instanceof FeignException)){
                log.error("Unexpected instance of HystrixRuntimeException has occurred", hystrixErr);
                throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }

            int causedByStatus = ((FeignException) causedBy).status();

            switch (causedByStatus){
                case 404:
                    log.debug("PHR client returned a 404 - NOT FOUND status, indicating no documents were found for the specified patientMrn", causedBy);
                    throw new NoDocumentsFoundException("No documents found for the specified patient");
                default:
                    log.error("PHR client returned an unexpected instance of FeignException", causedBy);
                    throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }
        }

        return uploadedDocuments;
    }

    @Override
    public Object getPatientDocumentByDocId(String patientMrn, Long id) {
        Object returnedDocument;

        // Assert mrn belong to current user
        enforceUserAuthForMrnService.assertCurrentUserAuthorizedForMrn(patientMrn);

        try {
            returnedDocument = phrUploadedDocumentsClient.getPatientDocumentByDocId(patientMrn, id);
        } catch (HystrixRuntimeException hystrixErr) {
            Throwable causedBy = hystrixErr.getCause();

            if(!(causedBy instanceof FeignException)){
                log.error("Unexpected instance of HystrixRuntimeException has occurred", hystrixErr);
                throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }

            int causedByStatus = ((FeignException) causedBy).status();

            switch(causedByStatus){
                case 404:
                    log.debug("PHR client returned a 404 - NOT FOUND status, indicating either no documents were found for the specified patientMrn," +
                            "or the document requested does not belong to the specified patientMrn", causedBy);
                    throw new NoDocumentsFoundException("No document found with the specified document ID");
                case 400:
                    log.error("PHR client returned a 400 - BAD REQUEST status, indicating invalid input was passed to PHR client", causedBy);
                    throw new InvalidInputException("Invalid input was passed to PHR client");
                default:
                    log.error("PHR client returned an unexpected instance of FeignException", causedBy);
                    throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }
        }

        return returnedDocument;
    }

    @Override
    public Object saveNewPatientDocument(String patientMrn, MultipartFile file, String documentName, String description, Long documentTypeCodeId) {
        Object returnedSavedDocument;

        // Assert mrn belong to current user
        enforceUserAuthForMrnService.assertCurrentUserAuthorizedForMrn(patientMrn);

        try{
            returnedSavedDocument = phrUploadedDocumentsClient.saveNewPatientDocument(patientMrn, file, documentName, description, documentTypeCodeId);
        } catch (HystrixRuntimeException hystrixErr) {
            Throwable causedBy = hystrixErr.getCause();

            if(!(causedBy instanceof FeignException)){
                log.error("Unexpected instance of HystrixRuntimeException has occurred", hystrixErr);
                throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }

            int causedByStatus = ((FeignException) causedBy).status();

            switch(causedByStatus){
                case 400:
                    log.error("PHR client returned a 400 - BAD REQUEST status, indicating invalid input was passed to PHR client", causedBy);
                    throw new InvalidInputException("Invalid input was passed to PHR client");
                case 412:
                    log.info("Document is invalid");
                    throw new DocumentInvalidException();
                case 409:
                    log.info("The specified patient already has a document with the same document name", causedBy);
                    throw new DocumentNameExistsException("The specified patient already has a document with the same document name");
                case 500:
                    log.error("An error occurred while attempting to save a new document", causedBy);
                    throw new DocumentSaveException("An error occurred while attempting to save a new document");
                default:
                    log.error("PHR client returned an unexpected instance of FeignException", causedBy);
                    throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }
        }

        return returnedSavedDocument;
    }

    @Override
    public void deletePatientDocument(String patientMrn, Long id) {
        // Assert mrn belong to current user
        enforceUserAuthForMrnService.assertCurrentUserAuthorizedForMrn(patientMrn);

        try {
            phrUploadedDocumentsClient.deletePatientDocument(patientMrn, id);
        } catch (HystrixRuntimeException hystrixErr) {
            Throwable causedBy = hystrixErr.getCause();

            if(!(causedBy instanceof FeignException)){
                log.error("Unexpected instance of HystrixRuntimeException has occurred", hystrixErr);
                throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }

            int causedByStatus = ((FeignException) causedBy).status();

            switch(causedByStatus){
                case 400:
                    log.error("PHR client returned a 400 - BAD REQUEST status, indicating invalid input was passed to PHR client", causedBy);
                    throw new InvalidInputException("Invalid input was passed to PHR client");
                case 404:
                    log.error("No documents were found with the specified document ID", causedBy);
                    throw new NoDocumentsFoundException("No document found with the specified document ID");
                case 500:
                    log.error("An error occurred while attempting to delete a document", causedBy);
                    throw new DocumentDeleteException("An error occurred while attempting to delete a document");
                default:
                    log.error("PHR client returned an unexpected instance of FeignException", causedBy);
                    throw new PhrClientInterfaceException("An unknown error occurred while attempting to communicate with PHR service");
            }
        }
    }
}