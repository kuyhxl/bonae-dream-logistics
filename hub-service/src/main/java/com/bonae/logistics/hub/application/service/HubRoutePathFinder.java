package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HubRoutePathFinder {

    public List<HubRouteEdge> findShortestPath(List<HubRouteEdge> activeRoutes, UUID departureHubId, UUID arrivalHubId) {
        Objects.requireNonNull(activeRoutes, "경로 탐색 전에 활성 이동정보 목록이 전달되어야 합니다.");
        Objects.requireNonNull(departureHubId, "경로 탐색 전에 검증된 출발 허브 ID가 전달되어야 합니다.");
        Objects.requireNonNull(arrivalHubId, "경로 탐색 전에 검증된 도착 허브 ID가 전달되어야 합니다.");

        // 1. 출발지와 도착지가 같으면 빈 경로 리스트 반환
        if (departureHubId.equals(arrivalHubId)) {
            return new ArrayList<>();
        }

        // 2. 인접 리스트(Graph) 구성
        // graph: 출발 허브 ID별로 해당 허브에서 나가는 간선을 저장한 인접 리스트
        Map<UUID, List<HubRouteEdge>> graph = new HashMap<>();

        for (HubRouteEdge edge : activeRoutes) {
            graph.computeIfAbsent(edge.fromHubId(), key -> new ArrayList<>()).add(edge);
        }

        // 3. 다익스트라 자료구조 초기화
        // durations: 출발 허브부터 각 허브까지 현재까지 발견한 최소 누적시간
        // previous: 각 허브의 현재 최단경로에서 직전에 사용한 간선
        // visited: 최소 누적시간이 확정되어 탐색을 마친 허브
        Map<UUID, Long> durations = new HashMap<>();
        Map<UUID, HubRouteEdge> previous = new HashMap<>();
        Set<UUID> visited = new HashSet<>();

        durations.put(departureHubId, 0L);

        // 4. 최단 경로 찾기
        while (true) {
            // current: 이번에 방문할 허브
            // minDuration: 현재까지 확인한 최소 누적시간
            UUID current = null;
            Long minDuration = null;

            // 발견한 허브 중 아직 방문하지 않은 최소 누적시간 허브를 선택한다.
            for (UUID id : durations.keySet()) {
                long currentDuration = durations.get(id);

                if (!visited.contains(id) && (minDuration == null || currentDuration < minDuration)) {
                    minDuration = currentDuration;
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

            List<HubRouteEdge> edges = graph.get(current);
            if (edges == null) {
                continue;
            }

            // 현재 허브에서 이동할 수 있는 인접 허브의 누적시간을 갱신한다.
            for (HubRouteEdge edge : edges) {
                UUID next = edge.toHubId();
                long newDuration = durations.get(current) + edge.durationSeconds();
                Long knownDuration = durations.get(next);

                // 처음 발견한 허브이거나 기존 값보다 누적시간이 짧으면 최단경로 정보를 갱신한다.
                if (knownDuration == null || newDuration < knownDuration) {
                    durations.put(next, newDuration);
                    previous.put(next, edge);
                }
            }
        }

        if (!visited.contains(arrivalHubId)) {
            throw new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        return reconstructPath(departureHubId, arrivalHubId, previous);
    }

    // 5. 역추적으로 최종 루트 완성
    List<HubRouteEdge> reconstructPath(UUID departureHubId, UUID arrivalHubId, Map<UUID, HubRouteEdge> previous) {
        List<HubRouteEdge> path = new ArrayList<>();
        UUID current = arrivalHubId;

        // 역추적 중 방문한 허브 저장
        Set<UUID> tracedHubIds = new HashSet<>();

        while (!current.equals(departureHubId)) {
            // Set.add()가 false면 이미 방문한 허브이므로 경로 체인의 순환으로 판단한다.
            if (!tracedHubIds.add(current)) {
                throw new IllegalStateException("경로 역추적 중 순환이 감지되었습니다. currentHubId=" + current);
            }

            HubRouteEdge edge = previous.get(current);

            if (edge == null) {
                throw new IllegalStateException("경로 역추적 중 이전 간선을 찾을 수 없습니다. currentHubId=" + current);
            }

            path.add(edge);
            current = edge.fromHubId();
        }

        // 도착→출발 순서로 수집했으므로 출발→도착 순서로 뒤집는다.
        Collections.reverse(path);

        return path;
    }
}