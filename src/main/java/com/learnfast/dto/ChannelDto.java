package com.learnfast.dto;

import com.learnfast.model.Channel;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelDto {
    private Long id;
    private String name;
    private Long creatorId;
    private String creatorName;
    private List<MemberInfo> members;

    public static ChannelDto from(Channel c) {
        ChannelDto d = new ChannelDto();
        d.id = c.getId();
        d.name = c.getName();
        d.creatorId = c.getCreator().getId();
        d.creatorName = c.getCreator().getName();
        d.members = c.getMembers().stream()
            .map(u -> new MemberInfo(u.getId(), u.getName(), u.getUsername(), u.getAvatarUrl()))
            .collect(Collectors.toList());
        return d;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getCreatorId() { return creatorId; }
    public String getCreatorName() { return creatorName; }
    public List<MemberInfo> getMembers() { return members; }

    public static class MemberInfo {
        public Long id;
        public String name;
        public String username;
        public String avatarUrl;
        MemberInfo(Long id, String name, String username, String avatarUrl) {
            this.id = id; this.name = name; this.username = username; this.avatarUrl = avatarUrl;
        }
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getAvatarUrl() { return avatarUrl; }
    }
}
