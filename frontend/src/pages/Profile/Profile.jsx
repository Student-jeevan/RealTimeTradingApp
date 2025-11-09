import { Badge } from '@/components/ui/badge'
import { Card ,CardTitle , CardHeader , CardContent } from '@/components/ui/card'
import { Dialog ,DialogClose,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,} from '@/components/ui/dialog'
import { VerifiedIcon } from 'lucide-react'
import React from 'react'
import { Button } from '@/components/ui/button'
import AccountVerificationForm from './AccountVerificationForm'
function Profile() {
    const handleEnableTwoStepVerification = () => {
           
        console.log('Two-step verification enabled');
      }
    return (
        <div className='flex flex-col items-center mb-5'>
            <div className='pt-10 w-full lg:w-[60%]'>
                <Card>
                    <CardHeader className='pb-9'>
                        <CardTitle>Your Information</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className='lg:flex gap-32'>
                            <div className='space-y-7'>
                                <div className='flex'>
                                    <p className='w-[9rem]'>Email:</p>
                                    <p className='text-gray-500'>jeevan@gmail.com</p>
                                </div>
                                <div className='flex'>
                                    <p className='w-[9rem]'>FullName:</p>
                                    <p className='text-gray-500'>jeevnsinghThitre</p>
                                </div>
                                <div className='flex'>
                                    <p className='w-[9rem]'>Date of Birth:</p>
                                    <p className='text-gray-500'>25/09/1919</p>
                                </div>
                                <div className='flex'>
                                    <p className='w-[9rem]'>Nationality:</p>
                                    <p className='text-gray-500'>Indian</p>
                                </div>
                            </div>
                             <div className='space-y-7'>
                                <div className='flex'>
                                    <p className='w-[9rem]'>Adress:</p>
                                    <p className='text-gray-500'>tadkal</p>
                                </div>
                                <div className='flex'>
                                    <p className='w-[9rem]'>City:</p>
                                    <p className='text-gray-500'>hyderabad</p>
                                </div>
                                <div className='flex'>
                                    <p className='w-[9rem]'>Passcode:</p>
                                    <p className='text-gray-500'>787444</p>
                                </div>
                                <div className='flex'>
                                    <p className='w-[9rem]'>Country:</p>
                                    <p className='text-gray-500'>Indian</p>
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </Card> 
                <div className='mt-6'>
                    <Card className='w-full'>
                        <CardHeader className='pb-7'>
                            <div className='flex items-center gap-3'>
                                <CardTitle>Two Step Verification</CardTitle>
                                    {true?<Badge className='bg-green-500'>
                                        <VerifiedIcon/>
                                        <span>Enabled</span>
                                       </Badge>:<Badge className='bg-green-500'>
                                        <VerifiedIcon/>
                                        <span>Enabled</span>
                                       </Badge>}
                                   
                            </div>
                        </CardHeader>
                        <CardContent>
                            <div>
                                <Dialog>
                                    <DialogTrigger>
                                        <Button>Enabled Two Step Verification</Button>
                                    </DialogTrigger>
                                    <DialogContent>
                                        <DialogHeader>
                                        <DialogTitle>Verify your account?</DialogTitle>
                                        </DialogHeader>
                                        <AccountVerificationForm handleSumbit={handleEnableTwoStepVerification}/>
                                    </DialogContent>
                                    </Dialog>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    )
}

export default Profile
