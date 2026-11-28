package com.dlog.asset.memo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetMemoService {

    private final AssetMemoRepository repository;

    public AssetMemoResponse get() {
        return repository.findById(AssetMemo.SINGLE_ID)
                .map(AssetMemoResponse::from)
                .orElseGet(AssetMemoResponse::empty);
    }

    /** 공백만 있으면 지운다 */
    @Transactional
    public AssetMemoResponse save(String content) {
        String text = content.strip();
        if (text.isEmpty()) {
            repository.deleteById(AssetMemo.SINGLE_ID);
            return AssetMemoResponse.empty();
        }
        AssetMemo memo = repository.findById(AssetMemo.SINGLE_ID)
                .map(m -> {
                    m.change(text);
                    return m;
                })
                .orElseGet(() -> repository.save(new AssetMemo(text)));
        return AssetMemoResponse.from(memo);
    }
}
