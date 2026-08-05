-- 논리 삭제되지 않은 허브의 이름 중복을 방지한다.
CREATE UNIQUE INDEX uk_p_hubs_active_name
    ON hub_service.p_hubs (name)
    WHERE deleted_at IS NULL;

-- 논리 삭제되지 않은 허브의 주소 중복을 방지한다.
CREATE UNIQUE INDEX uk_p_hubs_active_address
    ON hub_service.p_hubs (address)
    WHERE deleted_at IS NULL;

-- 활성 상태의 동일 방향 허브 간선 중복을 방지한다.
CREATE UNIQUE INDEX uk_p_hub_routes_active_pair
    ON hub_service.p_hub_routes (
                                 departure_hub_id,
                                 arrival_hub_id
        )
    WHERE deleted_at IS NULL;