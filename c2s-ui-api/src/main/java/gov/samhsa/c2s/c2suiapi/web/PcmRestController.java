package gov.samhsa.c2s.c2suiapi.web;

import gov.samhsa.c2s.c2suiapi.infrastructure.dto.*;
import gov.samhsa.c2s.c2suiapi.service.PcmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/pcm")
public class PcmRestController {

    @Autowired
    private PcmService pcmService;

    @GetMapping("/patients/providers")
    public List<FlattenedSmallProviderDto> getProviders() {
        return pcmService.getProviders();
    }

    @PostMapping("/patients/providers")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveProviders(@Valid @RequestBody IdentifiersDto providerIdentifiersDto) {
        pcmService.saveProviders(providerIdentifiersDto);
    }

    @DeleteMapping("/patients/providers/{providerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProvider(@PathVariable Long providerId) {
        pcmService.deleteProvider(providerId);
    }

    @GetMapping("/patients/consents")
    public DetailedConsentPageableDto getConsents(@RequestParam(value = "page", required = false) Integer page,
                                                  @RequestParam(value = "size", required = false) Integer size) {
        return pcmService.getConsents(page, size);
    }

    @PostMapping("/patients/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveConsent(@Valid @RequestBody ConsentDto consentDto) {
        pcmService.saveConsent(consentDto);
    }

    @DeleteMapping("/patients/consents/{consentId}")
    public void deleteConsent(@PathVariable Long consentId) {
        pcmService.deleteConsent(consentId);
    }

    @PutMapping("/patients/consents/{consentId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateConsent(@PathVariable Long consentId,
                              @Valid @RequestBody ConsentDto consentDto) {
        pcmService.updateConsent(consentId, consentDto);
    }

    @PutMapping("/patients/consents/{consentId}/attestation")
    @ResponseStatus(HttpStatus.OK)
    public void attestConsent(@PathVariable Long consentId,
                              @Valid @RequestBody ConsentAttestationDto consentAttestationDto) {
        pcmService.attestConsent(consentId, consentAttestationDto);
    }

    @PutMapping("/patients/consents/{consentId}/revocation")
    @ResponseStatus(HttpStatus.OK)
    public void revokeConsent(@PathVariable Long consentId,
                              @Valid @RequestBody ConsentRevocationDto consentRevocationDto) {
        pcmService.revokeConsent(consentId, consentRevocationDto);
    }

    @GetMapping("/purposes")
    public List<PurposeDto> getPurposes() {
        return pcmService.getPurposes();
    }
}