package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Request.CampaignRequestDTO;
import com.naal.bankmind.dto.Default.Response.CampaignDTO;
import com.naal.bankmind.entity.Default.MitigationCampaign;
import com.naal.bankmind.repository.Default.MitigationCampaignRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/morosidad/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final MitigationCampaignRepository campaignRepository;

    /**
     * GET /api/campaigns — Lista todas las campañas activas.
     */
    @GetMapping
    public ResponseEntity<List<CampaignDTO>> getAll() {
        List<CampaignDTO> campaigns = campaignRepository.findByIsActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(campaigns);
    }

    /**
     * POST /api/campaigns — Crear nueva campaña.
     */
    @PostMapping
    public ResponseEntity<CampaignDTO> create(@RequestBody CampaignRequestDTO request) {
        MitigationCampaign campaign = new MitigationCampaign();
        campaign.setCampaignName(request.getCampaignName());
        campaign.setDescription(request.getDescription());
        campaign.setTargetSegment(request.getTargetSegment());
        campaign.setReductionFactor(request.getReductionFactor());
        campaign.setEstimatedCost(request.getEstimatedCost());
        MitigationCampaign saved = campaignRepository.save(campaign);
        return ResponseEntity.ok(toDTO(saved));
    }

    /**
     * PUT /api/campaigns/{id} — Actualizar campaña existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CampaignDTO> update(@PathVariable Long id, @RequestBody CampaignRequestDTO request) {
        MitigationCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaña no encontrada: " + id));

        campaign.setCampaignName(request.getCampaignName());
        campaign.setDescription(request.getDescription());
        campaign.setTargetSegment(request.getTargetSegment());
        campaign.setReductionFactor(request.getReductionFactor());
        campaign.setEstimatedCost(request.getEstimatedCost());
        MitigationCampaign saved = campaignRepository.save(campaign);
        return ResponseEntity.ok(toDTO(saved));
    }

    /**
     * DELETE /api/campaigns/{id} — Soft-delete: desactiva la campaña.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        MitigationCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaña no encontrada: " + id));
        campaign.setIsActive(false);
        campaignRepository.save(campaign);
        return ResponseEntity.noContent().build();
    }

    // ---- Mapper ----

    private CampaignDTO toDTO(MitigationCampaign entity) {
        return new CampaignDTO(
                entity.getIdCampaign(),
                entity.getCampaignName(),
                entity.getDescription(),
                entity.getTargetSegment(),
                entity.getReductionFactor(),
                entity.getEstimatedCost(),
                entity.getIsActive(),
                entity.getCreatedDate());
    }
}
