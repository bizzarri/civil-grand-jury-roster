package com.mauri.juryroster;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.*;
import javax.crypto.spec.*;
public final class HandoffCrypto {
 private static final byte[] MAGIC="JURYROSTER1\n".getBytes(StandardCharsets.US_ASCII);
 public static byte[] encrypt(byte[] plain,char[] password)throws Exception{
  if(password.length<12)throw new IllegalArgumentException("Use a handoff passphrase of at least 12 characters.");
  byte[] salt=new byte[16],iv=new byte[12];new SecureRandom().nextBytes(salt);new SecureRandom().nextBytes(iv);
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),new GCMParameterSpec(128,iv));cipher.updateAAD(MAGIC);byte[] encrypted=cipher.doFinal(plain);
  byte[] out=new byte[MAGIC.length+28+encrypted.length];System.arraycopy(MAGIC,0,out,0,MAGIC.length);System.arraycopy(salt,0,out,MAGIC.length,16);System.arraycopy(iv,0,out,MAGIC.length+16,12);System.arraycopy(encrypted,0,out,MAGIC.length+28,encrypted.length);return out;
 }
 public static byte[] decrypt(byte[] packed,char[] password)throws Exception{
  if(packed.length<MAGIC.length+44||packed.length>8*1024*1024)throw new IllegalArgumentException("Not a supported handoff file.");
  for(int i=0;i<MAGIC.length;i++)if(packed[i]!=MAGIC[i])throw new IllegalArgumentException("Not a Jury Roster handoff file.");
  byte[] salt=Arrays.copyOfRange(packed,MAGIC.length,MAGIC.length+16),iv=Arrays.copyOfRange(packed,MAGIC.length+16,MAGIC.length+28);
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(password,salt),new GCMParameterSpec(128,iv));cipher.updateAAD(MAGIC);return cipher.doFinal(packed,MAGIC.length+28,packed.length-MAGIC.length-28);
 }
 private static SecretKeySpec key(char[] pass,byte[] salt)throws Exception{
  PBEKeySpec spec=new PBEKeySpec(pass,salt,210000,256);try{return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES");}finally{spec.clearPassword();}
 }
}
