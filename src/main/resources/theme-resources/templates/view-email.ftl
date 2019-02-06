<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        <div>${msg("loginTitle",(realm.displayName!''))}</div>
        <div>Check your email!</div>
    <#elseif section = "header">
        <div>${msg("loginTitleHtml",(realm.displayNameHtml!''))?no_esc}</div>
        <div>Check your email!</div>
    <#elseif section = "form">
        <#if realm.password>
            We've emailed a special link to you. Click the link to confirm your email address and access your account.
        </#if>
    </#if>
</@layout.registrationLayout>
