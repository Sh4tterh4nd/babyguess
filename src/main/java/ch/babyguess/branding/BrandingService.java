package ch.babyguess.branding;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.event.EventConfigurationRepository;
import ch.babyguess.event.StaleEventConfigurationException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BrandingService {

    private final EventConfigurationRepository configurationRepository;
    private final BrandingAssetStorage assetStorage;
    private final Clock clock;

    public BrandingService(
            EventConfigurationRepository configurationRepository,
            BrandingAssetStorage assetStorage,
            Clock clock) {
        this.configurationRepository = configurationRepository;
        this.assetStorage = assetStorage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BrandingPresentation presentation() {
        var configuration = configuration();
        return new BrandingPresentation(
                configuration.getTitle(),
                configuration.getVersion(),
                configuration.getLogoAssetName() != null,
                configuration.getBackgroundAssetName() != null);
    }

    @Transactional
    public void replace(long expectedVersion, BrandingAssetType type, MultipartFile upload) {
        var configuration = configuration();
        verifyVersion(configuration, expectedVersion);
        var oldAssetName = assetName(configuration, type);
        var newAssetName = assetStorage.store(upload);
        registerReplacementCleanup(oldAssetName, newAssetName);
        updateAssetName(configuration, type, newAssetName);
        configurationRepository.flush();
    }

    @Transactional
    public void remove(long expectedVersion, BrandingAssetType type) {
        var configuration = configuration();
        verifyVersion(configuration, expectedVersion);
        var oldAssetName = assetName(configuration, type);
        updateAssetName(configuration, type, null);
        configurationRepository.flush();
        if (oldAssetName != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    assetStorage.delete(oldAssetName);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public BrandingAssetContent activeAsset(BrandingAssetType type) {
        var name = assetName(configuration(), type);
        if (name == null) {
            throw new BrandingAssetNotFoundException();
        }
        return assetStorage.load(name);
    }

    private void registerReplacementCleanup(String oldAssetName, String newAssetName) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                assetStorage.delete(oldAssetName);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    assetStorage.delete(newAssetName);
                }
            }
        });
    }

    private void verifyVersion(EventConfiguration configuration, long expectedVersion) {
        if (configuration.getVersion() != expectedVersion) {
            throw new StaleEventConfigurationException();
        }
    }

    private void updateAssetName(
            EventConfiguration configuration,
            BrandingAssetType type,
            String assetName) {
        if (type == BrandingAssetType.LOGO) {
            configuration.updateLogoAssetName(assetName, clock.instant());
        } else {
            configuration.updateBackgroundAssetName(assetName, clock.instant());
        }
    }

    private String assetName(EventConfiguration configuration, BrandingAssetType type) {
        return type == BrandingAssetType.LOGO
                ? configuration.getLogoAssetName()
                : configuration.getBackgroundAssetName();
    }

    private EventConfiguration configuration() {
        return configurationRepository.findById(EventConfiguration.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Event configuration row is missing"));
    }
}
