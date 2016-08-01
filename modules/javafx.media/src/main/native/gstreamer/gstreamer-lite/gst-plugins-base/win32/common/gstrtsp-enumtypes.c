


#include "gstrtsp-enumtypes.h"

#include "gstrtspdefs.h"

/* enumerations from "gstrtspdefs.h" */
GType
gst_rtsp_result_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RTSP_OK, "GST_RTSP_OK", "ok"},
      {GST_RTSP_ERROR, "GST_RTSP_ERROR", "error"},
      {GST_RTSP_EINVAL, "GST_RTSP_EINVAL", "einval"},
      {GST_RTSP_EINTR, "GST_RTSP_EINTR", "eintr"},
      {GST_RTSP_ENOMEM, "GST_RTSP_ENOMEM", "enomem"},
      {GST_RTSP_ERESOLV, "GST_RTSP_ERESOLV", "eresolv"},
      {GST_RTSP_ENOTIMPL, "GST_RTSP_ENOTIMPL", "enotimpl"},
      {GST_RTSP_ESYS, "GST_RTSP_ESYS", "esys"},
      {GST_RTSP_EPARSE, "GST_RTSP_EPARSE", "eparse"},
      {GST_RTSP_EWSASTART, "GST_RTSP_EWSASTART", "ewsastart"},
      {GST_RTSP_EWSAVERSION, "GST_RTSP_EWSAVERSION", "ewsaversion"},
      {GST_RTSP_EEOF, "GST_RTSP_EEOF", "eeof"},
      {GST_RTSP_ENET, "GST_RTSP_ENET", "enet"},
      {GST_RTSP_ENOTIP, "GST_RTSP_ENOTIP", "enotip"},
      {GST_RTSP_ETIMEOUT, "GST_RTSP_ETIMEOUT", "etimeout"},
      {GST_RTSP_ETGET, "GST_RTSP_ETGET", "etget"},
      {GST_RTSP_ETPOST, "GST_RTSP_ETPOST", "etpost"},
      {GST_RTSP_ELAST, "GST_RTSP_ELAST", "elast"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_enum_register_static ("GstRTSPResult", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_event_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      {GST_RTSP_EV_READ, "GST_RTSP_EV_READ", "read"},
      {GST_RTSP_EV_WRITE, "GST_RTSP_EV_WRITE", "write"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_flags_register_static ("GstRTSPEvent", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_family_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RTSP_FAM_NONE, "GST_RTSP_FAM_NONE", "none"},
      {GST_RTSP_FAM_INET, "GST_RTSP_FAM_INET", "inet"},
      {GST_RTSP_FAM_INET6, "GST_RTSP_FAM_INET6", "inet6"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_enum_register_static ("GstRTSPFamily", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_state_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RTSP_STATE_INVALID, "GST_RTSP_STATE_INVALID", "invalid"},
      {GST_RTSP_STATE_INIT, "GST_RTSP_STATE_INIT", "init"},
      {GST_RTSP_STATE_READY, "GST_RTSP_STATE_READY", "ready"},
      {GST_RTSP_STATE_SEEKING, "GST_RTSP_STATE_SEEKING", "seeking"},
      {GST_RTSP_STATE_PLAYING, "GST_RTSP_STATE_PLAYING", "playing"},
      {GST_RTSP_STATE_RECORDING, "GST_RTSP_STATE_RECORDING", "recording"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_enum_register_static ("GstRTSPState", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_version_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RTSP_VERSION_INVALID, "GST_RTSP_VERSION_INVALID", "invalid"},
      {GST_RTSP_VERSION_1_0, "GST_RTSP_VERSION_1_0", "1-0"},
      {GST_RTSP_VERSION_1_1, "GST_RTSP_VERSION_1_1", "1-1"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_enum_register_static ("GstRTSPVersion", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_method_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      {GST_RTSP_INVALID, "GST_RTSP_INVALID", "invalid"},
      {GST_RTSP_DESCRIBE, "GST_RTSP_DESCRIBE", "describe"},
      {GST_RTSP_ANNOUNCE, "GST_RTSP_ANNOUNCE", "announce"},
      {GST_RTSP_GET_PARAMETER, "GST_RTSP_GET_PARAMETER", "get-parameter"},
      {GST_RTSP_OPTIONS, "GST_RTSP_OPTIONS", "options"},
      {GST_RTSP_PAUSE, "GST_RTSP_PAUSE", "pause"},
      {GST_RTSP_PLAY, "GST_RTSP_PLAY", "play"},
      {GST_RTSP_RECORD, "GST_RTSP_RECORD", "record"},
      {GST_RTSP_REDIRECT, "GST_RTSP_REDIRECT", "redirect"},
      {GST_RTSP_SETUP, "GST_RTSP_SETUP", "setup"},
      {GST_RTSP_SET_PARAMETER, "GST_RTSP_SET_PARAMETER", "set-parameter"},
      {GST_RTSP_TEARDOWN, "GST_RTSP_TEARDOWN", "teardown"},
      {GST_RTSP_GET, "GST_RTSP_GET", "get"},
      {GST_RTSP_POST, "GST_RTSP_POST", "post"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_flags_register_static ("GstRTSPMethod", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_auth_method_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RTSP_AUTH_NONE, "GST_RTSP_AUTH_NONE", "none"},
      {GST_RTSP_AUTH_BASIC, "GST_RTSP_AUTH_BASIC", "basic"},
      {GST_RTSP_AUTH_DIGEST, "GST_RTSP_AUTH_DIGEST", "digest"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstRTSPAuthMethod", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_header_field_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RTSP_HDR_INVALID, "GST_RTSP_HDR_INVALID", "invalid"},
      {GST_RTSP_HDR_ACCEPT, "GST_RTSP_HDR_ACCEPT", "accept"},
      {GST_RTSP_HDR_ACCEPT_ENCODING, "GST_RTSP_HDR_ACCEPT_ENCODING",
          "accept-encoding"},
      {GST_RTSP_HDR_ACCEPT_LANGUAGE, "GST_RTSP_HDR_ACCEPT_LANGUAGE",
          "accept-language"},
      {GST_RTSP_HDR_ALLOW, "GST_RTSP_HDR_ALLOW", "allow"},
      {GST_RTSP_HDR_AUTHORIZATION, "GST_RTSP_HDR_AUTHORIZATION",
          "authorization"},
      {GST_RTSP_HDR_BANDWIDTH, "GST_RTSP_HDR_BANDWIDTH", "bandwidth"},
      {GST_RTSP_HDR_BLOCKSIZE, "GST_RTSP_HDR_BLOCKSIZE", "blocksize"},
      {GST_RTSP_HDR_CACHE_CONTROL, "GST_RTSP_HDR_CACHE_CONTROL",
          "cache-control"},
      {GST_RTSP_HDR_CONFERENCE, "GST_RTSP_HDR_CONFERENCE", "conference"},
      {GST_RTSP_HDR_CONNECTION, "GST_RTSP_HDR_CONNECTION", "connection"},
      {GST_RTSP_HDR_CONTENT_BASE, "GST_RTSP_HDR_CONTENT_BASE", "content-base"},
      {GST_RTSP_HDR_CONTENT_ENCODING, "GST_RTSP_HDR_CONTENT_ENCODING",
          "content-encoding"},
      {GST_RTSP_HDR_CONTENT_LANGUAGE, "GST_RTSP_HDR_CONTENT_LANGUAGE",
          "content-language"},
      {GST_RTSP_HDR_CONTENT_LENGTH, "GST_RTSP_HDR_CONTENT_LENGTH",
          "content-length"},
      {GST_RTSP_HDR_CONTENT_LOCATION, "GST_RTSP_HDR_CONTENT_LOCATION",
          "content-location"},
      {GST_RTSP_HDR_CONTENT_TYPE, "GST_RTSP_HDR_CONTENT_TYPE", "content-type"},
      {GST_RTSP_HDR_CSEQ, "GST_RTSP_HDR_CSEQ", "cseq"},
      {GST_RTSP_HDR_DATE, "GST_RTSP_HDR_DATE", "date"},
      {GST_RTSP_HDR_EXPIRES, "GST_RTSP_HDR_EXPIRES", "expires"},
      {GST_RTSP_HDR_FROM, "GST_RTSP_HDR_FROM", "from"},
      {GST_RTSP_HDR_IF_MODIFIED_SINCE, "GST_RTSP_HDR_IF_MODIFIED_SINCE",
          "if-modified-since"},
      {GST_RTSP_HDR_LAST_MODIFIED, "GST_RTSP_HDR_LAST_MODIFIED",
          "last-modified"},
      {GST_RTSP_HDR_PROXY_AUTHENTICATE, "GST_RTSP_HDR_PROXY_AUTHENTICATE",
          "proxy-authenticate"},
      {GST_RTSP_HDR_PROXY_REQUIRE, "GST_RTSP_HDR_PROXY_REQUIRE",
          "proxy-require"},
      {GST_RTSP_HDR_PUBLIC, "GST_RTSP_HDR_PUBLIC", "public"},
      {GST_RTSP_HDR_RANGE, "GST_RTSP_HDR_RANGE", "range"},
      {GST_RTSP_HDR_REFERER, "GST_RTSP_HDR_REFERER", "referer"},
      {GST_RTSP_HDR_REQUIRE, "GST_RTSP_HDR_REQUIRE", "require"},
      {GST_RTSP_HDR_RETRY_AFTER, "GST_RTSP_HDR_RETRY_AFTER", "retry-after"},
      {GST_RTSP_HDR_RTP_INFO, "GST_RTSP_HDR_RTP_INFO", "rtp-info"},
      {GST_RTSP_HDR_SCALE, "GST_RTSP_HDR_SCALE", "scale"},
      {GST_RTSP_HDR_SESSION, "GST_RTSP_HDR_SESSION", "session"},
      {GST_RTSP_HDR_SERVER, "GST_RTSP_HDR_SERVER", "server"},
      {GST_RTSP_HDR_SPEED, "GST_RTSP_HDR_SPEED", "speed"},
      {GST_RTSP_HDR_TRANSPORT, "GST_RTSP_HDR_TRANSPORT", "transport"},
      {GST_RTSP_HDR_UNSUPPORTED, "GST_RTSP_HDR_UNSUPPORTED", "unsupported"},
      {GST_RTSP_HDR_USER_AGENT, "GST_RTSP_HDR_USER_AGENT", "user-agent"},
      {GST_RTSP_HDR_VIA, "GST_RTSP_HDR_VIA", "via"},
      {GST_RTSP_HDR_WWW_AUTHENTICATE, "GST_RTSP_HDR_WWW_AUTHENTICATE",
          "www-authenticate"},
      {GST_RTSP_HDR_CLIENT_CHALLENGE, "GST_RTSP_HDR_CLIENT_CHALLENGE",
          "client-challenge"},
      {GST_RTSP_HDR_REAL_CHALLENGE1, "GST_RTSP_HDR_REAL_CHALLENGE1",
          "real-challenge1"},
      {GST_RTSP_HDR_REAL_CHALLENGE2, "GST_RTSP_HDR_REAL_CHALLENGE2",
          "real-challenge2"},
      {GST_RTSP_HDR_REAL_CHALLENGE3, "GST_RTSP_HDR_REAL_CHALLENGE3",
          "real-challenge3"},
      {GST_RTSP_HDR_SUBSCRIBE, "GST_RTSP_HDR_SUBSCRIBE", "subscribe"},
      {GST_RTSP_HDR_ALERT, "GST_RTSP_HDR_ALERT", "alert"},
      {GST_RTSP_HDR_CLIENT_ID, "GST_RTSP_HDR_CLIENT_ID", "client-id"},
      {GST_RTSP_HDR_COMPANY_ID, "GST_RTSP_HDR_COMPANY_ID", "company-id"},
      {GST_RTSP_HDR_GUID, "GST_RTSP_HDR_GUID", "guid"},
      {GST_RTSP_HDR_REGION_DATA, "GST_RTSP_HDR_REGION_DATA", "region-data"},
      {GST_RTSP_HDR_MAX_ASM_WIDTH, "GST_RTSP_HDR_MAX_ASM_WIDTH",
          "max-asm-width"},
      {GST_RTSP_HDR_LANGUAGE, "GST_RTSP_HDR_LANGUAGE", "language"},
      {GST_RTSP_HDR_PLAYER_START_TIME, "GST_RTSP_HDR_PLAYER_START_TIME",
          "player-start-time"},
      {GST_RTSP_HDR_LOCATION, "GST_RTSP_HDR_LOCATION", "location"},
      {GST_RTSP_HDR_ETAG, "GST_RTSP_HDR_ETAG", "etag"},
      {GST_RTSP_HDR_IF_MATCH, "GST_RTSP_HDR_IF_MATCH", "if-match"},
      {GST_RTSP_HDR_ACCEPT_CHARSET, "GST_RTSP_HDR_ACCEPT_CHARSET",
          "accept-charset"},
      {GST_RTSP_HDR_SUPPORTED, "GST_RTSP_HDR_SUPPORTED", "supported"},
      {GST_RTSP_HDR_VARY, "GST_RTSP_HDR_VARY", "vary"},
      {GST_RTSP_HDR_X_ACCELERATE_STREAMING,
          "GST_RTSP_HDR_X_ACCELERATE_STREAMING", "x-accelerate-streaming"},
      {GST_RTSP_HDR_X_ACCEPT_AUTHENT, "GST_RTSP_HDR_X_ACCEPT_AUTHENT",
          "x-accept-authent"},
      {GST_RTSP_HDR_X_ACCEPT_PROXY_AUTHENT,
          "GST_RTSP_HDR_X_ACCEPT_PROXY_AUTHENT", "x-accept-proxy-authent"},
      {GST_RTSP_HDR_X_BROADCAST_ID, "GST_RTSP_HDR_X_BROADCAST_ID",
          "x-broadcast-id"},
      {GST_RTSP_HDR_X_BURST_STREAMING, "GST_RTSP_HDR_X_BURST_STREAMING",
          "x-burst-streaming"},
      {GST_RTSP_HDR_X_NOTICE, "GST_RTSP_HDR_X_NOTICE", "x-notice"},
      {GST_RTSP_HDR_X_PLAYER_LAG_TIME, "GST_RTSP_HDR_X_PLAYER_LAG_TIME",
          "x-player-lag-time"},
      {GST_RTSP_HDR_X_PLAYLIST, "GST_RTSP_HDR_X_PLAYLIST", "x-playlist"},
      {GST_RTSP_HDR_X_PLAYLIST_CHANGE_NOTICE,
            "GST_RTSP_HDR_X_PLAYLIST_CHANGE_NOTICE",
          "x-playlist-change-notice"},
      {GST_RTSP_HDR_X_PLAYLIST_GEN_ID, "GST_RTSP_HDR_X_PLAYLIST_GEN_ID",
          "x-playlist-gen-id"},
      {GST_RTSP_HDR_X_PLAYLIST_SEEK_ID, "GST_RTSP_HDR_X_PLAYLIST_SEEK_ID",
          "x-playlist-seek-id"},
      {GST_RTSP_HDR_X_PROXY_CLIENT_AGENT, "GST_RTSP_HDR_X_PROXY_CLIENT_AGENT",
          "x-proxy-client-agent"},
      {GST_RTSP_HDR_X_PROXY_CLIENT_VERB, "GST_RTSP_HDR_X_PROXY_CLIENT_VERB",
          "x-proxy-client-verb"},
      {GST_RTSP_HDR_X_RECEDING_PLAYLISTCHANGE,
            "GST_RTSP_HDR_X_RECEDING_PLAYLISTCHANGE",
          "x-receding-playlistchange"},
      {GST_RTSP_HDR_X_RTP_INFO, "GST_RTSP_HDR_X_RTP_INFO", "x-rtp-info"},
      {GST_RTSP_HDR_X_STARTUPPROFILE, "GST_RTSP_HDR_X_STARTUPPROFILE",
          "x-startupprofile"},
      {GST_RTSP_HDR_TIMESTAMP, "GST_RTSP_HDR_TIMESTAMP", "timestamp"},
      {GST_RTSP_HDR_AUTHENTICATION_INFO, "GST_RTSP_HDR_AUTHENTICATION_INFO",
          "authentication-info"},
      {GST_RTSP_HDR_HOST, "GST_RTSP_HDR_HOST", "host"},
      {GST_RTSP_HDR_PRAGMA, "GST_RTSP_HDR_PRAGMA", "pragma"},
      {GST_RTSP_HDR_X_SERVER_IP_ADDRESS, "GST_RTSP_HDR_X_SERVER_IP_ADDRESS",
          "x-server-ip-address"},
      {GST_RTSP_HDR_X_SESSIONCOOKIE, "GST_RTSP_HDR_X_SESSIONCOOKIE",
          "x-sessioncookie"},
      {GST_RTSP_HDR_RTCP_INTERVAL, "GST_RTSP_HDR_RTCP_INTERVAL",
          "rtcp-interval"},
      {GST_RTSP_HDR_KEYMGMT, "GST_RTSP_HDR_KEYMGMT", "keymgmt"},
      {GST_RTSP_HDR_LAST, "GST_RTSP_HDR_LAST", "last"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstRTSPHeaderField", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_rtsp_status_code_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RTSP_STS_INVALID, "GST_RTSP_STS_INVALID", "invalid"},
      {GST_RTSP_STS_CONTINUE, "GST_RTSP_STS_CONTINUE", "continue"},
      {GST_RTSP_STS_OK, "GST_RTSP_STS_OK", "ok"},
      {GST_RTSP_STS_CREATED, "GST_RTSP_STS_CREATED", "created"},
      {GST_RTSP_STS_LOW_ON_STORAGE, "GST_RTSP_STS_LOW_ON_STORAGE",
          "low-on-storage"},
      {GST_RTSP_STS_MULTIPLE_CHOICES, "GST_RTSP_STS_MULTIPLE_CHOICES",
          "multiple-choices"},
      {GST_RTSP_STS_MOVED_PERMANENTLY, "GST_RTSP_STS_MOVED_PERMANENTLY",
          "moved-permanently"},
      {GST_RTSP_STS_MOVE_TEMPORARILY, "GST_RTSP_STS_MOVE_TEMPORARILY",
          "move-temporarily"},
      {GST_RTSP_STS_SEE_OTHER, "GST_RTSP_STS_SEE_OTHER", "see-other"},
      {GST_RTSP_STS_NOT_MODIFIED, "GST_RTSP_STS_NOT_MODIFIED", "not-modified"},
      {GST_RTSP_STS_USE_PROXY, "GST_RTSP_STS_USE_PROXY", "use-proxy"},
      {GST_RTSP_STS_BAD_REQUEST, "GST_RTSP_STS_BAD_REQUEST", "bad-request"},
      {GST_RTSP_STS_UNAUTHORIZED, "GST_RTSP_STS_UNAUTHORIZED", "unauthorized"},
      {GST_RTSP_STS_PAYMENT_REQUIRED, "GST_RTSP_STS_PAYMENT_REQUIRED",
          "payment-required"},
      {GST_RTSP_STS_FORBIDDEN, "GST_RTSP_STS_FORBIDDEN", "forbidden"},
      {GST_RTSP_STS_NOT_FOUND, "GST_RTSP_STS_NOT_FOUND", "not-found"},
      {GST_RTSP_STS_METHOD_NOT_ALLOWED, "GST_RTSP_STS_METHOD_NOT_ALLOWED",
          "method-not-allowed"},
      {GST_RTSP_STS_NOT_ACCEPTABLE, "GST_RTSP_STS_NOT_ACCEPTABLE",
          "not-acceptable"},
      {GST_RTSP_STS_PROXY_AUTH_REQUIRED, "GST_RTSP_STS_PROXY_AUTH_REQUIRED",
          "proxy-auth-required"},
      {GST_RTSP_STS_REQUEST_TIMEOUT, "GST_RTSP_STS_REQUEST_TIMEOUT",
          "request-timeout"},
      {GST_RTSP_STS_GONE, "GST_RTSP_STS_GONE", "gone"},
      {GST_RTSP_STS_LENGTH_REQUIRED, "GST_RTSP_STS_LENGTH_REQUIRED",
          "length-required"},
      {GST_RTSP_STS_PRECONDITION_FAILED, "GST_RTSP_STS_PRECONDITION_FAILED",
          "precondition-failed"},
      {GST_RTSP_STS_REQUEST_ENTITY_TOO_LARGE,
            "GST_RTSP_STS_REQUEST_ENTITY_TOO_LARGE",
          "request-entity-too-large"},
      {GST_RTSP_STS_REQUEST_URI_TOO_LARGE, "GST_RTSP_STS_REQUEST_URI_TOO_LARGE",
          "request-uri-too-large"},
      {GST_RTSP_STS_UNSUPPORTED_MEDIA_TYPE,
          "GST_RTSP_STS_UNSUPPORTED_MEDIA_TYPE", "unsupported-media-type"},
      {GST_RTSP_STS_PARAMETER_NOT_UNDERSTOOD,
            "GST_RTSP_STS_PARAMETER_NOT_UNDERSTOOD",
          "parameter-not-understood"},
      {GST_RTSP_STS_CONFERENCE_NOT_FOUND, "GST_RTSP_STS_CONFERENCE_NOT_FOUND",
          "conference-not-found"},
      {GST_RTSP_STS_NOT_ENOUGH_BANDWIDTH, "GST_RTSP_STS_NOT_ENOUGH_BANDWIDTH",
          "not-enough-bandwidth"},
      {GST_RTSP_STS_SESSION_NOT_FOUND, "GST_RTSP_STS_SESSION_NOT_FOUND",
          "session-not-found"},
      {GST_RTSP_STS_METHOD_NOT_VALID_IN_THIS_STATE,
            "GST_RTSP_STS_METHOD_NOT_VALID_IN_THIS_STATE",
          "method-not-valid-in-this-state"},
      {GST_RTSP_STS_HEADER_FIELD_NOT_VALID_FOR_RESOURCE,
            "GST_RTSP_STS_HEADER_FIELD_NOT_VALID_FOR_RESOURCE",
          "header-field-not-valid-for-resource"},
      {GST_RTSP_STS_INVALID_RANGE, "GST_RTSP_STS_INVALID_RANGE",
          "invalid-range"},
      {GST_RTSP_STS_PARAMETER_IS_READONLY, "GST_RTSP_STS_PARAMETER_IS_READONLY",
          "parameter-is-readonly"},
      {GST_RTSP_STS_AGGREGATE_OPERATION_NOT_ALLOWED,
            "GST_RTSP_STS_AGGREGATE_OPERATION_NOT_ALLOWED",
          "aggregate-operation-not-allowed"},
      {GST_RTSP_STS_ONLY_AGGREGATE_OPERATION_ALLOWED,
            "GST_RTSP_STS_ONLY_AGGREGATE_OPERATION_ALLOWED",
          "only-aggregate-operation-allowed"},
      {GST_RTSP_STS_UNSUPPORTED_TRANSPORT, "GST_RTSP_STS_UNSUPPORTED_TRANSPORT",
          "unsupported-transport"},
      {GST_RTSP_STS_DESTINATION_UNREACHABLE,
          "GST_RTSP_STS_DESTINATION_UNREACHABLE", "destination-unreachable"},
      {GST_RTSP_STS_KEY_MANAGEMENT_FAILURE,
          "GST_RTSP_STS_KEY_MANAGEMENT_FAILURE", "key-management-failure"},
      {GST_RTSP_STS_INTERNAL_SERVER_ERROR, "GST_RTSP_STS_INTERNAL_SERVER_ERROR",
          "internal-server-error"},
      {GST_RTSP_STS_NOT_IMPLEMENTED, "GST_RTSP_STS_NOT_IMPLEMENTED",
          "not-implemented"},
      {GST_RTSP_STS_BAD_GATEWAY, "GST_RTSP_STS_BAD_GATEWAY", "bad-gateway"},
      {GST_RTSP_STS_SERVICE_UNAVAILABLE, "GST_RTSP_STS_SERVICE_UNAVAILABLE",
          "service-unavailable"},
      {GST_RTSP_STS_GATEWAY_TIMEOUT, "GST_RTSP_STS_GATEWAY_TIMEOUT",
          "gateway-timeout"},
      {GST_RTSP_STS_RTSP_VERSION_NOT_SUPPORTED,
            "GST_RTSP_STS_RTSP_VERSION_NOT_SUPPORTED",
          "rtsp-version-not-supported"},
      {GST_RTSP_STS_OPTION_NOT_SUPPORTED, "GST_RTSP_STS_OPTION_NOT_SUPPORTED",
          "option-not-supported"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstRTSPStatusCode", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
