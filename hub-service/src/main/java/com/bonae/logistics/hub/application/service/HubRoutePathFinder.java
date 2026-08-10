package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class HubRoutePathFinder {

    private final HubRouteRepository hubRouteRepository;

    @Transactional(readOnly = true)
    public List<HubRoute> findShortestPath(UUID departureHubId, UUID arrivalHubId) {
        Objects.requireNonNull(departureHubId, "출발 허브 ID는 null일 수 없습니다.");
        Objects.requireNonNull(arrivalHubId, "도착 허브 ID는 null일 수 없습니다.");

        // 1. 출발지와 도착지가 같으면 빈 경로 리스트 반환
        if (departureHubId.equals(arrivalHubId)) {
            return new ArrayList<>();
        }

        // 2. 인접 리스트(Graph) 구성
        List<HubRoute> routes = hubRouteRepository.findAllByDeletedAtIsNull();

        Map<UUID, List<HubRoute>> graph = new HashMap<>();
        for (HubRoute route : routes) {
            UUID from = route.getDepartureHub().getId();
            graph.computeIfAbsent(from, k -> new ArrayList<>()).add(route);
        }

        // 3. 다익스트라 자료구조 초기화
        Map<UUID, Integer> distance = new HashMap<>();
        Map<UUID, HubRoute> previous = new HashMap<>();
        Set<UUID> visited = new HashSet<>();

        distance.put(departureHubId, 0);

        // 4. 최단 경로 찾기
        while (true) {
            UUID current = null;
            int min = Integer.MAX_VALUE;

            // 아직 방문하지 않은 노드 중 최단 거리 노드 선택
            for (UUID id : distance.keySet()) {
                if (!visited.contains(id) && distance.get(id) < min) {
                    min = distance.get(id);
                    current = id;
                }
            }

            if (current == null) {
                break;
            }

            visited.add(current);

            if (current.equals(arrivalHubId)) {
                break;
            }

            List<HubRoute> edges = graph.get(current);
            if (edges == null) {
                continue;
            }

            // 인접 노드 갱신
            for (HubRoute edge : edges) {
                UUID next = edge.getArrivalHub().getId();
                int newDist = distance.get(current) + edge.getDurationSeconds();
                if (newDist < distance.getOrDefault(next, Integer.MAX_VALUE)) {
                    distance.put(next, newDist);
                    previous.put(next, edge);
                }
            }
        }

        if (!visited.contains(arrivalHubId)) {
            throw new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        // 5. 역추적으로 최종 루트 완성
        List<HubRoute> path = new ArrayList<>();
        UUID current = arrivalHubId;
        while (!current.equals(departureHubId)) {
            HubRoute edge = previous.get(current);
            path.add(edge);
            current = edge.getDepartureHub().getId();
        }
        Collections.reverse(path);

        return path;
    }
}