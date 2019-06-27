/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\workspaces\\androidstudio\\lenglian-module\\src\\main\\aidl\\com\\beetech\\module\\KeepAliveConnection.aidl
 */
package com.beetech.module;
// Declare any non-default types here with import statements

public interface KeepAliveConnection extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.beetech.module.KeepAliveConnection
{
private static final java.lang.String DESCRIPTOR = "com.beetech.module.KeepAliveConnection";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.beetech.module.KeepAliveConnection interface,
 * generating a proxy if needed.
 */
public static com.beetech.module.KeepAliveConnection asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.beetech.module.KeepAliveConnection))) {
return ((com.beetech.module.KeepAliveConnection)iin);
}
return new com.beetech.module.KeepAliveConnection.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
java.lang.String descriptor = DESCRIPTOR;
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(descriptor);
return true;
}
default:
{
return super.onTransact(code, data, reply, flags);
}
}
}
private static class Proxy implements com.beetech.module.KeepAliveConnection
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
}
}
}
